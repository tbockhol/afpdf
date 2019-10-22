/*  This file is a part of AFPDF - AFP/PDF transformer
Copyright (C) 2019  Sebis Direct, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.sebis.printing;

import java.util.HashMap;
import java.util.Map;

/**
 * CodePage names a collection of CodePoints (character id and numeric value)
 */
public class CodePage {
	
	String name;
	Map<String, Integer> codePoints;
	
	
	public CodePage(String name) {
		this.name = name;
		this.codePoints = new HashMap<String, Integer>();
	}
	
	
	public void addCodePoint(String gcgid, Integer value) {
		this.codePoints.put(gcgid, value);
	}
	
	public Integer getCodePoint(String gcgid) {
		if (this.codePoints.containsKey(gcgid)) {
			return this.codePoints.get(gcgid);
		} else {
			return null;
		}
	}
	
	public Map<String,Integer> getCodePoints() {
		return this.codePoints;
	}

}
