package net.takax3.twitter.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer
import java.io.IOException
import java.io.StringReader
import java.util.*

class Analyzer {
	
	fun generate(list: List<String>) : String {
		
		return generate(analyze(list))
		
	}
	
	
	fun generate(jsonObject: JsonObject) : String {
		
		val firstWord = jsonObject.getAsJsonArray("firstWord")
		val array = jsonObject.getAsJsonObject("array")
		
		var beforeWord = firstWord.get(Random().nextInt(firstWord.size())).toString().replace("\"", "")
		var outputText = beforeWord
		
		while (true) {
			val wordConnection = array.getAsJsonArray(beforeWord) ?: break
			val word = wordConnection.get(Random().nextInt(wordConnection.size())) ?: break
			if (word.toString() == "\"\"") break
			beforeWord = word.toString().replace("\"", "")
			outputText += beforeWord
		}
		
		return outputText
		
	}
	
	
	fun analyze(list: List<String>) : JsonObject {
		
		// 解析データ初期化
		val analyzedObject = JsonObject()
		analyzedObject.add("firstWord", JsonArray())
		analyzedObject.add("array", JsonObject())
		
		for (string in list) {
			val splitText: List<String> = split(string)
			
			if (splitText.isNotEmpty()) {
				val firstWord = analyzedObject.getAsJsonArray("firstWord")
				val array = analyzedObject.getAsJsonObject("array")
				firstWord.add(splitText[0])
				analyzedObject.add("firstWord", firstWord)
				
				for (j in 0 .. splitText.size-2) {
					var wordConnection = array.getAsJsonArray(splitText[j])
					if (wordConnection == null) wordConnection = JsonArray()
					wordConnection.add(splitText[j + 1])
					array.add(splitText[j], wordConnection)
				}
				var wordConnection = array.getAsJsonArray(splitText[splitText.size-1])
				if (wordConnection == null) wordConnection = JsonArray()
				wordConnection.add("")
				array.add(splitText[splitText.size-1], wordConnection)
				analyzedObject.add("array", array)
			}
			
		}
		
		return analyzedObject
		
	}
	
	fun split(string: String) : List<String> {
		val list = ArrayList<String>()
		try {
			JapaneseTokenizer(null, false, JapaneseTokenizer.Mode.NORMAL).use { japaneseTokenizer ->
				val charTermAttribute = japaneseTokenizer.addAttribute(CharTermAttribute::class.java)
				japaneseTokenizer.setReader(StringReader(string))
				japaneseTokenizer.reset()
				while (japaneseTokenizer.incrementToken()) {
					list.add(charTermAttribute.toString())
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
		
		return list
	}
	
}