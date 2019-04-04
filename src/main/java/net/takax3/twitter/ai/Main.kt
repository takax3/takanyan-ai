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
	
	private var config: Config? = null
	
	@JvmStatic
	fun main(args: Array<String>) {
		
		val readConsumerKeys = ReadConsumerKeys()
		
		if (!readConsumerKeys.read("ConsumerKey.json")) {
			return
		}
		
		consumerKey = readConsumerKeys.consumerKey!!
		consumerSecret = readConsumerKeys.consumerSecret!!
		
		val accessTokenFile = File("AccessToken.json")
		if (!accessTokenFile.exists()) {
			
			accessToken = OAuth().getAccessToken(consumerKey, consumerSecret)
			
			if (accessToken !is AccessToken) {
				return
			}
			
			try {
				val configAccessToken = ConfigAccessToken()
				configAccessToken.accessToken = accessToken!!.token
				configAccessToken.accessSecret = accessToken!!.tokenSecret
				
				val fileWriter = FileWriter(accessTokenFile)
				
				fileWriter.write(gson.toJson(configAccessToken))
				
				fileWriter.close()
				
				accessToken = AccessToken(configAccessToken.accessToken, configAccessToken.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		} else {
			try {
				val fileReader = FileReader(accessTokenFile)
				
				val configAccessToken = gson.fromJson(fileReader, ConfigAccessToken::class.java)
				fileReader.close()
				
				accessToken = AccessToken(configAccessToken.accessToken, configAccessToken.accessSecret)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
			
		}
		
		twitter = TwitterFactory().instance
		twitter!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitter!!.oAuthAccessToken = accessToken
		
		val configFile = File("Config.json")
		if(!configFile.exists()) {
			try {
				config = Config()
				val fileWriter = FileWriter(configFile)
				fileWriter.write(gson.toJson(config))
				fileWriter.close()
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
		} else {
			try {
				val fileReader = FileReader(configFile)
				config = gson.fromJson(fileReader, Config::class.java)
				fileReader.close()
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
		}
		
		while (true) {
			
			val output = analyze()
			
			// 出力
			println(output)
			twitter!!.updateStatus(output)
			
			Thread.sleep((config!!.intervalMinute!! * 60 * 1000).toLong())
			
		}
		
	}
	
	
	private fun analyze() : String{
		
		// 解析データ初期化
		val analyzedObject = JsonObject()
		analyzedObject.add("firstWord", JsonArray())
		analyzedObject.add("array", JsonObject())
		
		// 解析元データ取得
		val paging = Paging()
		paging.count = 200
		
		for (i in 0 .. config!!.collectTweetNum!! / 200) {
			if (i == config!!.collectTweetNum!! / 200) {
				if (config!!.collectTweetNum!! % 200 == 0) break else paging.count = config!!.collectTweetNum!! % 200
			}
			val timeline = twitter!!.getUserTimeline(config!!.ownerUserID!!, paging)
			
			// 一件一件の解析
			for (status in timeline) {
				val text = status.text
				// リプライ、URL付、タグツイを除外
				if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1 && status.user.id != twitter!!.id) {
					
					println("------------------------")
					println(status.text)
					val list: List<String> = split(text)
					println(list)
					
					if (list.isNotEmpty()) {
						val firstWord = analyzedObject.getAsJsonArray("firstWord")
						val array = analyzedObject.getAsJsonObject("array")
						firstWord.add(list[0])
						analyzedObject.add("firstWord", firstWord)
						
						for (j in 0 .. list.size-2) {
							var wordConnection = array.getAsJsonArray(list[j])
							if (wordConnection == null) wordConnection = JsonArray()
							wordConnection.add(list[j + 1])
							array.add(list[j], wordConnection)
						}
						var wordConnection = array.getAsJsonArray(list[list.size-1])
						if (wordConnection == null) wordConnection = JsonArray()
						wordConnection.add("")
						array.add(list[list.size-1], wordConnection)
						analyzedObject.add("array", array)
					}
					
				}
				paging.maxId = status.id
			}
			
		}
		
		// デバッグ用の解析データ出力
		println(gson.toJson(analyzedObject))
//		val fileWriter = FileWriter(File("word.json"))
//		fileWriter.write(gson.toJson(analyzedObject))
//		fileWriter.close()
		
		
		// 解析したデータから文章を生成
		val firstWord = analyzedObject.getAsJsonArray("firstWord")
		val array = analyzedObject.getAsJsonObject("array")
		
		var beforeWord = firstWord.get(Random().nextInt(firstWord.size())).toString().replace("\"", "")
		var output = beforeWord
		
		while (true) {
			val wordConnection = array.getAsJsonArray(beforeWord) ?: break
			val word = wordConnection.get(Random().nextInt(wordConnection.size())) ?: break
			if (word.toString().replace("\"", "") == "") break
			beforeWord = word.toString().replace("\"", "")
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
	
	class Config {
		
		var ownerUserID: String? = "takax3_M"
		var intervalMinute: Int? = 15
		var collectTweetNum: Int? = 1000
		
	}
	
}