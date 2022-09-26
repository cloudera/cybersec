/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.bro;

import org.apache.metron.stellar.common.JSONMapObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONCleaner implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * @param jsonString
	 * @return
	 * Takes a json String as input and modifies the keys to remove any characters other than . _ a-z A-Z or 0-9
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public JSONMapObject clean(String jsonString)
	{
		Map json = new JSONMapObject(jsonString);
		JSONMapObject output = new JSONMapObject();
	    Iterator iter = json.entrySet().iterator();

		 while(iter.hasNext()){
		      Map.Entry entry = (Map.Entry)iter.next();
		      
		      String key = ((String)entry.getKey()).replaceAll("[^\\._a-zA-Z0-9]+","");
		      output.put(key, entry.getValue());
		    }

		return output;
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public static void main(String args[])
	{
		String jsonText = "{\"first_1\": 123, \"second\": [4, 5, 6], \"third\": 789}";
		JSONCleaner cleaner = new JSONCleaner();
		try {
			//cleaner.clean(jsonText);
			Map obj=new HashMap();
			  obj.put("name","foo");
			  obj.put("num", 100);
			  obj.put("balance", 1000.21);
			  obj.put("is_vip", true);
			  obj.put("nickname",null);
			Map obj1 = new HashMap();
			obj1.put("sourcefile", obj);

			JSONMapObject json = new JSONMapObject(obj1);
			System.out.println(json);
			  
			  
			  
			  System.out.print(jsonText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
