package net.takax3.twitter.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer
import java.io.IOException
import java.io.StringReader
import java.util.*
import kotlin.run

class Analyzer {
	
	fun generate(list: List<String>): String {
		
		return generate(analyze(list))
		
	}
	
	
	fun generate(jsonObject: JsonObject): String {
		
		val firstWord = jsonObject.getAsJsonArray("firstWord")
		val array = jsonObject.getAsJsonObject("array")
		
		var beforeWord = firstWord.get(Random().nextInt(firstWord.size())).asString
		var outputText = beforeWord
		
		while (true) {
			val wordConnection = array.getAsJsonArray(beforeWord) ?: break
			val word = wordConnection.get(Random().nextInt(wordConnection.size())) ?: break
			if (word.asString == "") break
			beforeWord = word.asString
			outputText += beforeWord
		}
		
		return outputText
		
	}
	
	
	fun analyze(list: List<String>, analyzedObject: JsonObject = JsonObject(), depth: Int = 1): JsonObject {
		
		// 解析データ初期化
		analyzedObject.run {
			if (!has("depth")) {
				addProperty("depth", depth)
			} else if (get("depth").asInt != depth) {
				remove("depth")
				remove("firstWord")
				remove("array")
				addProperty("depth", depth)
				add("firstWord", JsonArray())
				add("array", JsonObject())
			}
			if (!has("firstWord")) add("firstWord", JsonArray())
			if (!has("array")) add("array", JsonObject())
		}
		
		for (string in list) {
			val splitText: List<String> = split(string)
			
			if (splitText.isNotEmpty()) {
				
				analyzedObject.add("firstWord", analyzedObject.getAsJsonArray("firstWord").apply {
					add(splitText[0])
				})
				
				analyzedObject.add("array", analyzedObject.getAsJsonObject("array").apply {
					repeat(splitText.size - 1) {
						add(splitText[it], (getAsJsonArray(splitText[it]) ?: JsonArray()).apply { add(splitText[it + 1]) })
					}
					add(splitText[splitText.size - 1], (getAsJsonArray(splitText[splitText.size - 1]) ?: JsonArray()).apply { add("") })
				})
				
			}
			
		}
		
		return analyzedObject
		
	}
	
	fun split(string: String): List<String> {
		val list = ArrayList<String>()
		try {
			JapaneseTokenizer(null, false, JapaneseTokenizer.Mode.NORMAL).use { japaneseTokenizer ->
				japaneseTokenizer.run {
					val charTermAttribute = addAttribute(CharTermAttribute::class.java)
					setReader(StringReader(string))
					reset()
					while (incrementToken()) {
						list.add(charTermAttribute.toString())
					}
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
		
		return list
	}
	
}