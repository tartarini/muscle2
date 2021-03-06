/*
Copyright 2008,2009 Complex Automata Simulation Technique (COAST) consortium

GNU Lesser General Public License

This file is part of MUSCLE (Multiscale Coupling Library and Environment).

    MUSCLE is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MUSCLE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/

package muscle.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
static methods for miscellaneous stuff
@author Jan Hegewald
*/
public class FileTool {

	private final static String HOME_PREFIX = "~" + System.getProperty("file.separator");
	
	public static <A> String joinItems(List<A> list, String separator) {

		StringBuilder joined = null;
		
		for( A item : list ) {
			if( joined == null ) {
				joined = new StringBuilder(item.toString());
			}
			else {
				joined.append(separator);
				joined.append(item.toString());
			}
		}
		
		return joined == null ? null : joined.toString();
	}
	
	public static File joinPaths(String ... paths) {
		if (paths.length == 0) {
			return null;
		}
		File basePath = resolveTilde(paths[0]);
		
		for(int i = 1; i < paths.length; i++ ) {
			basePath = new File(basePath, paths[i]);
		}
		
		return basePath;
	}
	
	/**
	try to expand tilde at beginning of path
	returns the unmodified path if there is no tilde at the beginning
	*/
	public static File resolveTilde(String path) {
		if(path.startsWith(HOME_PREFIX)) {
			return new File(System.getProperty("user.home"), path.substring(HOME_PREFIX.length()));
		} else if(path.equals("~")) {
			return new File(System.getProperty("user.home"));
		} else {
			return new File(path);
		}
	}

	public static String fileToString(File file, String commentIndicator) throws IOException, java.io.FileNotFoundException {

		StringBuilder fileData = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		while( (line = reader.readLine() ) != null) {
			if( !line.trim().startsWith(commentIndicator)) {
				fileData.append(line);
				fileData.append("\n");
			}
		}
		
		reader.close();
		
		return fileData.toString();
	}
	
	
	// forbidden ASCII chars
	//		windows: [/\:*?"<>|]
	//		mac: [/]
	private final static Pattern filenameRegex = Pattern.compile("[/\\:*?\"<>|]");

	public static String portableFileName(String rawName, String replacement) {
		return filenameRegex.matcher(rawName).replaceAll(replacement);
	}
	
	// In Java 7 this is integrated in Files.createSymbolicLink(), but Java 7 is not installed everywhere.
	public static boolean createSymlink(File name, File target) {
		try {
			Runtime.getRuntime().exec(new String[] {"ln", "-s", target.getPath(), name.getAbsolutePath()});
			return true;
		} catch (IOException ex) {
			Logger.getLogger(FileTool.class.getName()).log(Level.WARNING, "Could not create symlink from " + name + " to " + target + ".", ex);
			return false;
		}
	}
}
