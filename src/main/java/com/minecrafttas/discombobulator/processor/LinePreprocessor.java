package com.minecrafttas.discombobulator.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.minecrafttas.discombobulator.utils.Pair;

/*Welcome to the madness that is this preprocessor. Here I will try as best as I can to explain how this works.
 * Why am I explaining it? Because my hope is, that at least I can remember what the hell I was doing when I made this
 * 
 * Start of with the "preprocess" method which is the only public method here.
 */

/**
 * The main preprocessor component
 * @author Scribble
 *
 */
public class LinePreprocessor {
	private final Pattern regexBlocks = Pattern.compile("^\\s*\\/\\/ *(#+) *(.+)");
	private final Pattern regexHashtag = Pattern.compile("^\\s*# *(#+) *(.+)");
	private final Pattern regexPatterns = Pattern.compile("^.+\\/\\/ *@(.+);");

	private List<String> versions;
	private Map<String, Map<String, String>> patterns;

	/**
	 * If the list should be inverted with the first one being the default
	 */
	private final boolean inverted;

	/**
	 * Debug linecount for errors during preprocessing
	 */
	private int linenumber = 0;
	private Boolean versionEnabled = true;

	/**
	 * Creates a new processor. The default will be the lowest version.
	 * 
	 * <pre>
	 * [
	 * "1.20",
	 * "1.19",
	 * "1.18",
	 * "1.16" <- default version since it's the lowest
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public LinePreprocessor(List<String> versions, Map<String, Map<String, String>> patterns) {
		this(versions, patterns, false);
	}

	/**
	 * Creates a new processor. The order can be inverted.
	 * <p> If inverted is true:
	 * 
	 * <pre>
	 * [
	 * "1.20", <- default version since it's the highest
	 * "1.19",
	 * "1.18",
	 * "1.16"
	 * ]
	 * </pre>
	 * @param versions The versions to check for in an order
	 * @param patterns The patterns to check for in no specific order
	 */
	public LinePreprocessor(List<String> versions, Map<String, Map<String, String>> patterns, boolean inverted) {
		if (versions == null) {
			throw new NullPointerException("Versions can't be null!");
		}
		this.versions = versions;
		this.patterns = patterns;
		this.inverted = inverted;
	}

	/**
	 * Preprocesses the lines to a targeted version.
	 * <p>Preprocessing can happen in 2 ways:<br>
	 * <ol>
	 * <li>Preprocessing a block of code</li>
	 * <li>Preprocessing a specific line of code with patterns</li>
	 * </ol>
	 * <p>The advantage of pattern processing is that you can save yourself some work making a version block for common patterns like <code>Minecraft.getMinecraft().player</code> in 1.12 to <code>Minecraft.getMinecraft().thePlayer</code> in e.g. 1.10
	 *
	 * <h2>VersionBlock</h2>
	 * <p>A version block allows you to uncomment part of the code in certain versions via comments:
	 * <pre>
	 * //# 1.12.2
	 * 	The code you want to write in 1.12.2
	 * //# 1.9.4
	 * //$$	The code you want to write in 1.9.4
	 * //# def
	 * //$$	Default code that is below 1.9.4
	 * //# end
	 * </pre>
	 * <p>Blocks start with a //# mcversion and end with a //# end. If the targeted version is not defined in the block, the next lowest version will be chosen for preprocessing (e.g. target is 1.11, doesn't exist, so 1.9.4 block will be preprocessed).<br>
	 * You can define a default block that defaults to the lowest version you specified (e.g. 1.9.2 is lower than 1.9.4, no other version is lower, so it uses default). <br>
	 * 
	 * <p>You can define these blocks out of order (e.g. first 1.9.4 then 1.12.2)... The version order is ultimately defined in the build.gradle.
	 * 
	 * <h3>Nesting</h3>
	 * 
	 * <p>Sometimes you have a lot of changes in one version but a tiny amount is added in the following versions. In these cases, you would need to copy all the large changes again into the newest version:
	 * 
	 * <pre>
	 * //# 1.16.5
	 * New things in 1.16.5
	 * 
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * 
	 * //# 1.16.1
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * //# end
	 * </pre>
	 * 
	 * <p>Here you need to double the code to achieve your goal. An easier way can be achieved with nesting.
	 * <p>You can define a nested block inside a version block by adding hashtags to the definition:
	 * 
	 * <pre>
	 * 
	 * //# 1.16.1
	 * New things in 1.16.1
	 * that are a bit longer
	 * than the changes in
	 * 1.16.5
	 * 
	 * //## 1.16.5		<-- 2 hashtags define the nested version
	 * New things in 1.16.5
	 * //## end		<-- The end of the nested block is defined with 2 hashtags as well
	 * 
	 * //# end
	 * </pre>
	 * 
	 * <p>To nest inside a nesting block you can add more hashtags.
	 * 
	 * <p>Some rules to follow:
	 * <ul>
	 * <li>Every nested block needs an 'end' statement</li>
	 * <li>The nested version can't be lower (if inverted, higher) than the parent version.<br>
	 * In the example above, you can't nest 1.15.2 inside a 1.16.1 block.</li>
	 * <li>You cannot skip nesting levels. Defining level 3 nesting after a level 1 nesting is not allowed</li>
	 * <li>Using def in a nested block will use the parent version</li>
	 * </ul>
	 * 
	 * <h2>Patterns</h2>
	 * Patterns are defined in the build.gradle:
	 * 
	 * <pre>
	 * patterns = [
	 * 	GetWindow: [
	 *		"def": "mc.window",
	 *		"1.15.2": "mc.getWindow()"
	 *	]
	 * ]
	 * </pre>
	 * <p> Each pattern has a name and multiple versions, with a default under "def".<br>
	 * To apply patterns, we have to write a <code>//@ patternname;</code> in the line we want to apply. Note that the pattern is terminated via semicolon.
	 * 
	 * <pre>
	 * mc.window; // @GetWindow;
	 * </pre>
	 * 
	 * <p>We can also apply multiple patterns in one line, seperated via comma:
	 * 
	 * <pre>
	 * Minecraft.getMinecraft().setWindow(mc.window); // @GetWindow,GetMinecraft;
	 * </pre>
	 * 
	 * <p>Similar to version blocks, if you specify a targetVersion for a pattern that does not specify that version, 
	 * the next lowest (or highest if inverted) version will be chosen as a pattern, the default pattern if nothing was found.
	 * 
	 * @param targetVersion The version for which lines should be enabled
	 * @param lines The lines to preprocess
	 * @param filename Debug filename for errors during preprocessing
	 * @return The preprocessed lines of the file
	 * @throws Exception 
	 */
	public List<String> preprocess(String targetVersion, List<String> lines, String fileending) throws Exception {
		List<String> out = new ArrayList<>();
		this.linenumber = 0;

		if (targetVersion == null) { // Enable the default version if targetversion is null. This is currently used when updating the base folder
			int defaultIndex = getIndex("def");
			targetVersion = versions.get(defaultIndex);
		}

		boolean useHashtags = shouldUseHashTag(fileending);
		ConcurrentLinkedQueue<Boolean> enabledQueue = new ConcurrentLinkedQueue<>();

		for (String line : lines) {
			linenumber++;

			// ====== Version Blocks

			if (containsStatement(line, useHashtags)) {
				if (enabledQueue.isEmpty()) {
					enabledQueue = generateEnabledQueue(lines, useHashtags, targetVersion);
				}
			}

			line = preprocessVersionBlock(line, targetVersion, enabledQueue, useHashtags);

			// ====== Patterns

			if (patterns != null) {
				line = preprocessPattern(line, targetVersion);
			}

			// =====================

			out.add(line);
		}

		return out;
	}

	/*========================================================
	__      __           _               ____  _            _    
	\ \    / /          (_)             |  _ \| |          | |   
	 \ \  / /__ _ __ ___ _  ___  _ __   | |_) | | ___   ___| | __
	  \ \/ / _ \ '__/ __| |/ _ \| '_ \  |  _ <| |/ _ \ / __| |/ /
	   \  /  __/ |  \__ \ | (_) | | | | | |_) | | (_) | (__|   < 
	    \/ \___|_|  |___/_|\___/|_| |_| |____/|_|\___/ \___|_|\_\
	                                                          
	==========================================================
	
	*
	* This section contains everything about version block preprocessing.
	* 
	*/

	/**
	 * <p>Searches through the text in advance to generate a queue that tells {@linkplain #preprocessVersionBlock(String, String, ConcurrentLinkedQueue, boolean)} if that block should be enabled or not.
	 * 
	 * <p>Here are the 4 steps this method runs through:
	 * 
	 * <h2>1.Generate the blockList</h2>
	 * Generates a list of version blocks by prereading the block. To take nesting into account, it generates the lists recursively. Most of the error checking is done here.
	 * <pre>
	 * 1.16.1
	 * 	1.16.5	<- (Nested versions)
	 * 	1.17.1
	 * 	end
	 * 1.18.1
	 * def
	 * end
	 * </pre>
	 * <h2>2.Sort by version index</h2>
	 * For knowing what versions we have to enable, we first need to correctly sort the versions in the order as they appear in {@link #versions}
	 * <pre>
	 * 1.18.1
	 * 1.16.1
	 * 	1.17.1
	 * 	1.16.5
	 * 	end
	 * def
	 * end
	 * </pre>
	 * <h2>3.Enabling the version</h2>
	 * Now we can correctly assign an enabled value to each version including the def and end blocks
	 * <pre>
	 * =======Target Version: 1.17.1
	 * 1.18.1: false
	 * 1.16.1: true
	 * 	1.17.1: true
	 * 	1.16.5: false
	 * 	end: true
	 * def: false
	 * end: true
	 * </pre>
	 * <p>Note that the last end has to be true, since everything outside a version block list is enabled by default
	 * <pre>
	 * public void example() {	<-Outside a block, so line is not commented out by default
	 * // # 1.16.1
	 * //CODE!
	 * // # end
	 * }	<-Outside a block, so line is not commented out by default
	 * </pre>
	 * <h2>4.Sort by appearence</h2>
	 * After assigning the state, we need to return to the initial state before sorting. To do this, we sort out list again by appearence.
	 * <pre>
	 * =======Target Version: 1.17.1
	 * 1.16.1: true
	 * 	1.16.5: false
	 * 	1.17.1: true
	 * 	end: true
	 * 1.18.1: false
	 * def: false
	 * end: true
	 * </pre>
	 * <h2>Generating a queue from the versions</h2>
	 * Now we can read out the booleans recursively and add them to a queue
	 * <pre>
	 * true,
	 * false,
	 * true,
	 * true,
	 * false,
	 * false,
	 * true
	 * </pre>
	 * This can be used in {@link #preprocessVersionBlock(String, String, ConcurrentLinkedQueue, boolean)} to enable or disable the lines.
	 * @param lines
	 * @param useHashTag
	 * @return A queue with booleans matching the number of version statements in the version block
	 * @throws Exception 
	 */
	private ConcurrentLinkedQueue<Boolean> generateEnabledQueue(List<String> lines, boolean useHashTag, String targetVersion) throws Exception {

		if (targetVersion != null && !versions.contains(targetVersion)) {
			throw new RuntimeException(String.format("The target version %s was not found in line %s", targetVersion, linenumber));
		}

		VersionBlockList blockList = generateBlockList(lines, linenumber - 1, useHashTag, 1, null).right();
		blockList.sortByVersionIndex();
		blockList.setEnabledVersion(targetVersion);
		blockList.sortByAppearence();
//		System.out.println("=======================Target: "+targetVersion+"\n"+blockList);		// Enable this to print the blocks
		return blockList.getQueue();
	}

	/**
	 * Parses the lines with the given start line recursively and returns a {@link VersionBlockList} with {@link VersionBlock} inside.<br>
	 * Those VersionBlocks still have nested version lists
	 * @param lines The total lines in this file
	 * @param startLine The starting line of when to search
	 * @param useHashTag If a hashtags for comments should be used instead of //
	 * @param parentNestingLevel The nesting level. Increases with each recursion step
	 * @return A pair with the line count on left, and the block numbers on right. Line count is used to skip the already processed lines
	 * @throws Exception If there are rule violations described in {@link #preprocess(String, List, String, String)}
	 */
	private Pair<Integer, VersionBlockList> generateBlockList(List<String> lines, int startLine, boolean useHashTag, int parentNestingLevel, VersionBlock parent) throws Exception {

		VersionBlockList blockList = new VersionBlockList();
		int index = 0;
		int lineCount;

		VersionBlock currentBlock = null;

		/*Reading ahead and storing the version statements in blockList*/
		for (lineCount = startLine; lineCount < lines.size(); lineCount++) {

			String line = lines.get(lineCount);

			/*Generate different matchers depending if the file uses hashtag as comments or not*/
			Matcher matcher;
			if (!useHashTag) {
				matcher = regexBlocks.matcher(line);
			} else {
				matcher = regexHashtag.matcher(line);
			}

			if (!matcher.find()) { // Skip if it can't find a version statement in this line. e.g. //# 1.12.2
				continue;
			}

			/*Read the version statement*/
			String version = matcher.group(2);
			int level = matcher.group(1).length(); // Nesting level of this version statement

			/*Error checking*/
			if (!version.equals("end") && !version.equals("def") && !versions.contains(version)) {
				throw new RuntimeException(String.format("The specified version %s in line %s was not found", version, lineCount + 1));
			}

			if (lineCount == startLine && version.equals("end")) {
				throw new Exception(String.format("Unexpected 'end' found in line %s", lineCount + 1));
			}

			/*Nesting*/
			Pair<Integer, VersionBlockList> nestedVersions = null;

			if (level == parentNestingLevel + 1) { // If the new nesting level is higher than the nesting level of the parent block

				checkForNestingErrors(version, currentBlock.version, level, lineCount);

				nestedVersions = generateBlockList(lines, lineCount, useHashTag, level, currentBlock); // Start recursion to generate the blockList in the next nesting level
				lineCount = nestedVersions.left(); // nestedVersions.left()=lineCount from nested versions. Since we already processed these lines in the recursion we can skip these lines here.

				if (!nestedVersions.right().isEmpty()) {
					currentBlock.addNestedVersionBlockList(nestedVersions.right()); // Add nested versions to the parent block
				}
			} else if (level == parentNestingLevel) { // If the level stays the same
				if (currentBlock != null) {
					index++; // Increase the blockList index

					blockList.addBlock(currentBlock);
				}
				currentBlock = new VersionBlock(parent, version, index, level);
			} else if (level == parentNestingLevel - 1) {
				throw new Exception(String.format("Missing an end for nesting before line %s", lineCount + 1));
			} else {
				throw new Exception(String.format("Unexpected nesting level in line %s", lineCount + 1));
			}

			if (blockList.contains(version)) {
				throw new Exception(String.format("Duplicate version definition %s found in line %s", version, lineCount + 1));
			}

			/*End condition*/
			if (version.equals("end") && level == parentNestingLevel) {
				blockList.addBlock(currentBlock);
				break;
			}

		}
		return Pair.of(lineCount, blockList);
	}

	private int getIndex(String version) {
		int versionIndex = versions.indexOf(version);
		if ("def".equals(version)) {
			if (!inverted)
				versionIndex = versions.size() - 1;
			else
				versionIndex = 0;
		}
		return versionIndex;
	}

	/**
	 * <p>A structural component which defines single version block
	 * 
	 * <p>A "version block" is in this case one preprocessor definition like this:
	 * <pre>
	 * // # 1.16.1
	 * </pre>
	 * 
	 * <p>In this case, the version is 1.16.1, the nesting level is 1.<br>
	 * Note that the "def" and "end" keywords are also considered as a "version block" with special behaviour.
	 * 
	 * <p>A version can be disabled or enabled, after the list generation was completed in {@link VersionBlockList#setEnabledVersion(String)}
	 * 
	 * <h2>Nesting</h2>
	 * 
	 * <p>A version block can include one or more {@linkplain VersionBlockList} to define nested versions:
	 * 
	 * <pre>
	 * String version;
	 * // # 1.16.1
	 * version = "1.16.1";
	 * --------------------
	 * |// ## 1.16.5      |
	 * |version = "1.16.5"| <- A VersionBlockList inside a VersionBlock
	 * |// ## end         |
	 * --------------------
	 * 
	 * --------------------
	 * |// ## 1.17.1      |
	 * |version = "1.17.1"|
	 * |// ## def         | <- Second VersionBlockList
	 * |version = "1.16.1"|
	 * |// ## end         |
	 * --------------------
	 * //# end
	 * </pre>
	 * <p>These nested blocks are defined in {@link #nestedBlockLists}. Each of the version blocks also has it's parent block stored as well.
	 * 
	 * @author Scribble
	 *
	 */
	private class VersionBlock {
		/**
		 * The string version of this version block.
		 */
		private String version;
		/**
		 * The index in the {@linkplain VersionBlockList} it is contained in. Used in {@link #sortByAppearence()}
		 */
		private int index;
		/**
		 * The nesting level this VerisonBlock has. Currently only used for printing purposes.
		 */
		private int level;
		/**
		 * The nested {@linkplain VersionBlockList}s
		 */
		private List<VersionBlockList> nestedBlockLists = new ArrayList<>();
		/**
		 * Whether this VersionBlock is marked as enabled or not. These can be set later, for example after sorting the list.
		 */
		private boolean enabled;
		/**
		 * The parent version block, used for getting parent information in the nesting block. Null if this block is in the first nesting level and doesn't have a parent.
		 */
		private VersionBlock parent;

		/**
		 * @see VersionBlock
		 * @param parent
		 * @param version
		 * @param index
		 * @param level
		 */
		public VersionBlock(VersionBlock parent, String version, int index, int level) {
			this.parent = parent;
			this.version = version;
			this.index = index;
			this.level = level;
		}

		/**
		 * Adds a {@linkplain VersionBlockList} as a child block list to this VersionBlock.
		 * @param blocks
		 */
		public void addNestedVersionBlockList(VersionBlockList blocks) {
			nestedBlockLists.add(blocks);
		}

		@Override
		public String toString() {
			String out = "   ".repeat(level - 1) + version + ": " + enabled + "\n";
			for (VersionBlockList list : nestedBlockLists) {
				out = out.concat(list.toString());
			}
			return out;
		}

		/**
		 * Recursively gets the size
		 * @return The number of version block statements
		 */
		public int size() {
			int i = 1; // This block
			for (VersionBlockList list : nestedBlockLists) {
				i = i + list.size();
			}
			return i;
		}

		/**
		 * Returns the version index of this versionBlock used for sorting.
		 * 
		 * For example if {@linkplain LinePreprocessor#versions} contains:
		 * 
		 * <pre>
		 * 1.18.1
		 * 1.16.1
		 * 1.14.4
		 * </pre>
		 * 
		 * <p>The following outputs would occur depending on {@link #version}:
		 * <pre>
		 * 1.18.1 -> 0
		 * 1.16.1 -> 1
		 * 1.14.4 -> 2
		 * 
		 * end -> 3 or -1 if inverted
		 * 
		 * def -> 2, 0 if inverted, or the parent version if parent is not null
		 * </pre>
		 * 
		 * 
		 * @return The index of the specified version from {@linkplain LinePreprocessor#versions}
		 */
		public int getVersionIndex() {
			if (version.equals("end")) { // Special behaviour if the version is "end"
				if (!inverted)
					return versions.size();
				else
					return -1;
			}
			if (version.equals("def") && parent != null) { // If this is a nested block and version is def, use the parent version. This also works recursively
				return parent.getVersionIndex();
			}
			return getIndex(version);
		}

		//====================================== Sorting

		/**
		 * Sorts the {@link #nestedBlockLists} by version index recursively
		 * @see VersionBlockList#sortByVersionIndex()
		 */
		public void sortByVersionIndex() {
			for (VersionBlockList list : nestedBlockLists) {
				list.sortByVersionIndex();
			}
		}

		/**
		 * Sorts the {@link #nestedBlockLists} by {@link #index} recursively
		 * @see VersionBlockList#sortByAppearence()
		 */
		public void sortByAppearence() {
			for (VersionBlockList list : nestedBlockLists) {
				list.sortByAppearence();
			}
		}

		//====================================== Enable/Disable

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setEnabledVersion(String targetVersion) {
			for (VersionBlockList versionBlockList : nestedBlockLists) {
				versionBlockList.setEnabledVersion(targetVersion);
			}
		}

		//====================================== Queue Generation
		/**
		 * @return A queue with enabled/disabled versions from {@link #nestedBlockLists}, generated recursively
		 */
		public ConcurrentLinkedQueue<Boolean> getQueue() {
			ConcurrentLinkedQueue<Boolean> out = new ConcurrentLinkedQueue<>();
			for (VersionBlockList block : nestedBlockLists) {
				out.addAll(block.getQueue());
			}
			return out;
		}

	}

	/**
	 * <p>A list of {@linkplain VersionBlock}s. Version block lists start with a version block of any version and end with a version block of "end":
	 * <pre>
	 * --------------------------------
	 * |// # 1.16.5 <- Version Block  |
	 * |Code for 1.16.5               |
	 * |// # 1.15.2 <- Version Block  | <- Version Block List
	 * |Code for 1.15.2               |
	 * |// # end <-VersionBlock       |
	 * --------------------------------
	 * </pre>
	 * <p>The version block list does not contain any nested blocks, since they are store in their respective version blocks and therefore have a parent.
	 * 
	 * @author Scribble
	 *
	 */
	private class VersionBlockList {
		private List<VersionBlock> blocks = new ArrayList<>();

		public void addBlock(VersionBlock block) {
			blocks.add(block);
		}

		public boolean isEmpty() {
			return blocks.isEmpty();
		}

		@Override
		public String toString() {
			String out = "";
			for (VersionBlock block : blocks) {
				out = out.concat(block.toString());
			}
			return out;
		}

		public int size() {
			int i = 0;
			for (VersionBlock block : blocks) {
				i = i + block.size();
			}
			return i;
		}

		public boolean contains(String version) {
			for (VersionBlock block : blocks) {
				if (block.version.equals(version)) {
					return true;
				}
			}
			return false;
		}

		//====================================== Sorting

		/**
		 * Sorts all blocks by the order of when they appear in {@linkplain LinePreprocessor#versions}
		 */
		public void sortByVersionIndex() {
			blocks.sort((left, right) -> {
				int compare = Integer.compare(left.getVersionIndex(), right.getVersionIndex());
				if (inverted)
					compare = Integer.compare(right.getVersionIndex(), left.getVersionIndex());
				return compare;
			});
			for (VersionBlock block : blocks) {
				block.sortByVersionIndex(); // Start of recursive sorting
			}
		}

		/**
		 * Sorts all blocks by the order of the {@linkplain VersionBlock#index}
		 */
		public void sortByAppearence() {
			blocks.sort((left, right) -> {
				int compare = Integer.compare(left.index, right.index);
				return compare;
			});
			for (VersionBlock block : blocks) {
				block.sortByAppearence(); // Start of recursive sorting
			}
		}

		//====================================== Enable/Disable

		/**
		 * Main logic of enabeling/disabeling versions and searching through the list
		 * @param targetVersion
		 */
		public void setEnabledVersion(String targetVersion) {
			int targetIndex = getIndex(targetVersion);

			boolean found = false;

			for (VersionBlock block : blocks) {

				int currentIndex = block.getVersionIndex();

				boolean enabled = false; // Whether the current block is enabled or not

				if (block.version.equals("end")) { // If the version equals end
					if (block.parent != null) {
						enabled = block.parent.enabled; // If we are inside a nested block, we will return to the parent block after the end, 
														// therefore we have to set it enabled according to the parent version
					} else {
						enabled = true; // If this block is the last end in the list, set it to true. See javadoc for generateEnableQueue under Step 3
					}
				}

				else if (targetVersion == null) { // If the targetVersion is null, nothing inside a version block should be enabled
					enabled = false;
				}

				else if (block.parent != null && !block.parent.enabled) { // If the parent is disabled, the nested block should not be enabled as well.
					enabled = false;
				}

				else { // Searching for the target version
					if ((currentIndex < targetIndex && !inverted) || (currentIndex > targetIndex && inverted)) { // If the target is still too high/too low continue searching
						enabled = false;
					} else if (currentIndex == targetIndex) { // If there is a target version matching, set it to found
						enabled = true;
						found = true;
					} else {
						if (!found) {
							enabled = true;
							found = true;
						} else {
							enabled = false;
						}
					}
				}
				block.setEnabled(enabled);
				block.setEnabledVersion(targetVersion); // Start of recursive actions
			}
		}

		//====================================== Queue Generation

		public ConcurrentLinkedQueue<Boolean> getQueue() {
			ConcurrentLinkedQueue<Boolean> out = new ConcurrentLinkedQueue<Boolean>();
			for (VersionBlock block : blocks) {
				out.add(block.enabled);
				out.addAll(block.getQueue());
			}

			return out;
		}
		//========================================================
	}

	private boolean shouldUseHashTag(String fileending) {
		return "accesswidener".equals(fileending);
	}

	private String preprocessVersionBlock(String line, String targetVersion, ConcurrentLinkedQueue<Boolean> enabledQueue, boolean useHashTag) throws Exception {
		if (containsStatement(line, useHashTag)) {
			versionEnabled = enabledQueue.poll();
			return line;
		}
		if (versionEnabled == null) {
			versionEnabled = true;
		}
		return enableLine(line, versionEnabled, useHashTag);
	}

	/**
	 * @param line The line to check
	 * @param useHashTags Whether to search for hashtags or // as comments
	 * @return If this line contains a versionBlock statement
	 */
	private boolean containsStatement(String line, boolean useHashTags) {
		return (useHashTags && Pattern.matches(regexHashtag.pattern(), line)) || (!useHashTags && Pattern.matches(regexBlocks.pattern(), line));
	}

	/**
	 * Comments out or uncomments a line.
	 * @param line The line to change
	 * @param enable True if line should be enabled
	 * @return The new line
	 */
	private String enableLine(String line, boolean enable, boolean useHashTag) {

		String commentChars = "//";
		if (useHashTag) {
			commentChars = "#";
		}

		boolean isDisabled = line.startsWith(commentChars + "$$");
		if (enable)
			return isDisabled ? line.replace(commentChars + "$$", "") : line;
		else
			return isDisabled ? line : commentChars + "$$" + line;
	}

	private void checkForNestingErrors(String nestedVer, String parentVer, int nestingLevel, int lineCount) throws Exception {

		if (nestedVer.equals("end")) {
			throw new Exception(String.format("Unexpected 'end' in nested block found in line %s", lineCount + 1));
		}

		int nestedIndex = getIndex(nestedVer);
		int parentIndex = getIndex(parentVer);

		if (nestedIndex > parentIndex && !inverted && !nestedVer.equals("def")) {
			throw new Exception(String.format("The version in the nesting block is smaller than in the parent block. Nested: %s, Parent: %s, Line: %s", nestedVer, parentVer, lineCount + 1));
		} else if (nestedIndex < parentIndex && inverted && !nestedVer.equals("def")) {
			throw new Exception(String.format("The version in the nesting block is greater than in the parent block. Nested: %s, Parent: %s, Line: %s", nestedVer, parentVer, lineCount + 1));
		}
	}

	/*========================================================
		  _____      _   _                      
		 |  __ \    | | | |                     
		 | |__) |_ _| |_| |_ ___ _ __ _ __  ___ 
		 |  ___/ _` | __| __/ _ \ '__| '_ \/ __|
		 | |  | (_| | |_| ||  __/ |  | | | \__ \
		 |_|   \__,_|\__|\__\___|_|  |_| |_|___/
	                                                          
	==========================================================
	
	* Everything related to patterns can be found here.
	*/

	private String preprocessPattern(String line, String targetVersion) {

		Matcher match = this.regexPatterns.matcher(line);
		if (!match.find()) {
			return line;
		}

		// find pattern
		String patternNames = match.group(1);
		List<Map<String, String>> patterns = getPatterns(patternNames);

		for (Map<String, String> pattern : patterns) { // Iterate through multiple patterns
			String replacement = findReplacement(pattern, targetVersion);
			if (replacement == null) {
				throw new RuntimeException(String.format("The specified pattern %s in line %s was not found for target version %s", patternNames, linenumber, targetVersion));
			}

			if (line.contains(replacement)) { // Optimization, if the targetversion is already the correct
				return line;
			}

			String replaceable = null;

			for (Entry<String, String> entry : pattern.entrySet()) {
				if (line.contains(entry.getValue())) {
					replaceable = entry.getValue();
					break;
				}
			}

			if (replaceable == null)
				throw new RuntimeException(String.format("The specified pattern %s in line %s was not found for any version", patternNames, line));

			line = line.replace(replaceable, replacement);
		}

		return line;
	}

	/**
	 * Split the pattern names and get the patterns
	 * @param patternnames Names to split
	 * @return A list of mapped patterns
	 */
	private List<Map<String, String>> getPatterns(String patternnames) {

		List<Map<String, String>> out = new ArrayList<>();

		String[] split = patternnames.split(","); // Split the names

		for (String names : split) {
			names = names.trim(); // trim any spaces

			Map<String, String> pattern = this.patterns.get(names);
			if (pattern == null) {
				System.out.println(String.format("The specified pattern %s in line %s was not found", names, linenumber));
				continue;
			}
			out.add(pattern);
		}
		return out;
	}

	/**
	 * Searches through the patterns to find the replacement text for any given target version.
	 * 
	 * If the version is not in the patterns, it finds the next lowest (highest if {@link #inverted} is true) version.
	 * 
	 * If the version is too low it uses the replacement defined in the "def" block.
	 * 
	 * If no version and no def block was found it returns null.
	 * 
	 * @param pattern The pattern to search through
	 * @param targetVersion The version we want the replacement for
	 * @return The string that should replace the pattern in the line. Null if no pattern was found.
	 */
	private String findReplacement(Map<String, String> pattern, String targetVersion) {

		String replacement = pattern.get(targetVersion);

		if (replacement != null) { // Optimization if the target version has a matching pattern
			return replacement;
		}

		int targetIndex = this.versions.indexOf(targetVersion); // Get the targetIndex

		if (!inverted) {
			replacement = searchPatterns(targetIndex, pattern);
		} else {
			replacement = searchPatternsInverted(targetIndex, pattern);
		}

		if (replacement == null) { // If there was no version found
			if (pattern.containsKey("def")) { // Use default as replacement
				replacement = pattern.get("def");
			}
		}

		return replacement;
	}

	/**
	 * Search through versions lower than the targetVersion for a fitting version.
	 * 
	 * The higher the index, the lower the version. 
	 * This loop starts at the lowest version and goes higher up to one below the target version
	 * 
	 * @param targetIndex The index we want to stop on
	 * @param pattern The pattern to search through
	 * @return The string that should replace the pattern in the line
	 */
	private String searchPatterns(int targetIndex, Map<String, String> pattern) {
		String replacement = null;

		for (int currentIndex = versions.size() - 1; currentIndex > targetIndex; currentIndex--) {
			String currentVersion = versions.get(currentIndex);

			if (pattern.containsKey(currentVersion)) {
				replacement = pattern.get(currentVersion);
			}
		}
		return replacement;
	}

	/**
	 * Search through versions higher than the targetVersion for a fitting version.
	 * 
	 * The higher the index, the lower the version. 
	 * This loop starts at the highest version and goes lower up to one above the target version
	 * 
	 * @param targetIndex The index we want to stop on
	 * @param pattern The pattern to search through
	 * @return The string that should replace the pattern in the line
	 */
	private String searchPatternsInverted(int targetIndex, Map<String, String> pattern) {
		String replacement = null;

		for (int currentIndex = 0; currentIndex < targetIndex; currentIndex++) {
			String currentVersion = versions.get(currentIndex);

			if (pattern.containsKey(currentVersion)) {
				replacement = pattern.get(currentVersion);
			}
		}
		return replacement;
	}
}
