package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

object WordPreProcessor {
	
	var lastReadTime = 0L
	var excludeWordsList = ArrayList<String>()
	
	fun isIncludeExcludeWords(word: String, filename: String = "ExcludeWords.json"): Boolean {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		
		File(filename).run {
			if (!exists()) {
				FileWriter(this).run {
					write(gson.toJson(JsonArray().apply {
						add("#")
						add("@")
						add("://")
					}))
					close()
				}
			}
			
			if(lastModified() > lastReadTime) {
				excludeWordsList = ArrayList()
				gson.fromJson(FileReader(this), JsonArray::class.java).run {
					for (json in this) excludeWordsList.add(json.asString)
				}
			}
		}
		
		for (excludeWord in excludeWordsList) {
			if (word.contains(excludeWord)) return true
		}
		return false
	}
	
}