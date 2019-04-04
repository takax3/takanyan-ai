package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer
import java.io.*
import java.io.IOException
import java.io.StringReader
import java.util.*


object Main {
	
	private var consumerKey = ""
	private var consumerSecret = ""
	
	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	private var twitter: Twitter? = null
	private var accessToken: AccessToken? = null
	
	private const val WAIT_MINUTE = 15
	
	
	@JvmStatic
	fun main(args: Array<String>) {
		
		val readConsumerKeys = ReadConsumerKeys()
		
		if (!readConsumerKeys.read("ConsumerKey.json")) {
			return
		}
		
		consumerKey = readConsumerKeys.consumerKey!!
		consumerSecret = readConsumerKeys.consumerSecret!!
		
		val file = File("AccessToken.json")
		if (!file.exists()) {
			
			accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
			
			if (accessToken !is AccessToken) {
				return
			}
			
			try {
				val config = ConfigAccessToken()
				config.accessToken = accessToken!!.token
				config.accessSecret = accessToken!!.tokenSecret
				
				val fileWriter = FileWriter(file)
				
				fileWriter.write(gson.toJson(config))
				
				fileWriter.close()
				
				accessToken = AccessToken(config.accessToken, config.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		} else {
			try {
				val fileReader = FileReader(file)
				
				val config = gson.fromJson(fileReader, ConfigAccessToken::class.java)
				fileReader.close()
				
				accessToken = AccessToken(config.accessToken, config.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		}
		
		twitter = TwitterFactory().instance
		twitter!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitter!!.oAuthAccessToken = accessToken
		
		
		while (true) {
			
			val output = analyze()
			
			// 出力
			println(output)
			twitter!!.updateStatus(output)
			
			Thread.sleep((WAIT_MINUTE * 60 * 1000).toLong())
			
		}
		
	}
	
	
	private fun analyze() : String{
		
		// 解析元データ取得
		val paging = Paging()
		paging.count = 200
		val timeline = twitter!!.getHomeTimeline(paging)
		
		// 解析データ初期化
		val word = JsonObject()
		word.add("firstWord", JsonArray())
		word.add("length", JsonArray())
		word.add("array", JsonObject())
		
		// 一件一件の解析
		for (status in timeline) {
			val text = status.text
			// リプライ、URL付、タグツイを除外
			if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1 && status.user.id != twitter!!.id) {
				
				println("------------------------")
				println(status.text)
				val list: List<String> = split(text)
				println(list)
				
				val firstWord = word.getAsJsonArray("firstWord")
				val length = word.getAsJsonArray("length")
				val array = word.getAsJsonObject("array")
				firstWord.add(list[0])
				length.add(list.size)
				word.add("firstWord", firstWord)
				
				for (i in 0 .. list.size-2) {
					var wordConnection = array.getAsJsonArray(list[i])
					if (wordConnection == null) wordConnection = JsonArray()
					wordConnection.add(list[i + 1])
					array.add(list[i], wordConnection)
				}
				word.add("array", array)
				
			}
		}
		
		// デバッグ用の解析データ出力
		println(gson.toJson(word))
//		val fileWriter = FileWriter(File("word.json"))
//		fileWriter.write(gson.toJson(word))
//		fileWriter.close()
		
		
		// 解析したデータから文章を生成
		val firstWord = word.getAsJsonArray("firstWord")
		val length = word.getAsJsonArray("length")
		val array = word.getAsJsonObject("array")
		
		var beforeWord = firstWord.get(Random().nextInt(firstWord.size())).toString().replace("\"", "")
		var output = beforeWord
		
		for (i in 1..length.get(Random().nextInt(length.size())).asInt) {
			val wordConnection = array.getAsJsonArray(beforeWord) ?: break
			beforeWord = wordConnection.get(Random().nextInt(wordConnection.size())).toString().replace("\"", "")
			output += beforeWord
		}
		
		return output
		
	}
	
	
	private fun split(string: String) : List<String> {
		val list = ArrayList<String>()
		try {
			JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL).use { japaneseTokenizer ->
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
	
	
	class ConfigAccessToken {
		
		var accessToken: String? = null
		var accessSecret: String? = null
		
	}
	
}