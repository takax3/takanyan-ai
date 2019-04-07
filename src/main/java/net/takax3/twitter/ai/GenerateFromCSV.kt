package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.opencsv.bean.CsvToBeanBuilder
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object GenerateFromCSV {
	
	private var consumerKey = ""
	private var consumerSecret = ""
	
	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	private var twitter: Twitter? = null
	private var accessToken: AccessToken? = null
	
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
		
		val tweetsCSVFile = File("tweets.csv")
		val tweetsJSONFile = File("tweets.json")
		val analyzedWords: JsonObject
		
		if (!tweetsCSVFile.exists()) {
			println("tweets.csv is not found...")
			return
		}
		
		if (!tweetsJSONFile.exists()) {
			println("解析済みファイルが存在しません。")
			println("解析を開始します。")
			val tweets: List<Tweet> = CsvToBeanBuilder<Tweet>(FileReader(tweetsCSVFile)).withType(Tweet::class.java).build().parse()
			val source = ArrayList<String>()
			for (tweet in tweets) {
				val text = tweet.text!!
				if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1) {
					println("------------------------")
					println(text)
					source.add(text)
				}
			}
			println("計 ${source.size} 件のツイートを処理します。")
			
			analyzedWords = Analyzer().analyze(source)
			
			val fileWriter = FileWriter(tweetsJSONFile)
			fileWriter.write(gson.toJson(analyzedWords))
			fileWriter.close()
		} else if (tweetsJSONFile.lastModified() < tweetsCSVFile.lastModified()) {
			println("解析済みファイルが元ファイルより古いです。")
			println("解析を開始します。")
			val tweets: List<Tweet> = CsvToBeanBuilder<Tweet>(FileReader(tweetsCSVFile)).withType(Tweet::class.java).build().parse()
			val source = ArrayList<String>()
			for (tweet in tweets) {
				val text = tweet.text!!
				if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1 && tweet.source!!.indexOf("UserReport") == -1) {
					println("------------------------")
					println(text)
					source.add(text)
				}
			}
			println("計 ${source.size} 件のツイートを処理します。")
			
			analyzedWords = Analyzer().analyze(source)
			
			FileWriter(tweetsJSONFile).write(gson.toJson(analyzedWords))
		} else {
			println("解析済みファイルから処理します。")
			try {
				analyzedWords = gson.fromJson(FileReader(tweetsJSONFile), JsonObject::class.java)
			} catch (e: Exception) {
				e.printStackTrace()
				return
			}
		}
		
		
		while (true) {
			val word = Analyzer().generate(analyzedWords)
			twitter!!.updateStatus("$word\r\n(Source Full Ver)")
			println("$word\r\n(Source Full Ver)")
			Thread.sleep(15 * 60 * 1000)
		}
		
	}
	
	
	class ConfigAccessToken {
		
		var accessToken: String? = null
		var accessSecret: String? = null
		
	}
	
	class Tweet {
		
		var tweet_id: String? = null
		var in_reply_to_status_id: String? = null
		var in_reply_to_user_id: String? = null
		var timestamp: String? = null
		var source: String? = null
		var text: String? = null
		var retweeted_status_id: String? = null
		var retweeted_status_user_id: String? = null
		var retweeted_status_timestamp: String? = null
		var expanded_urls: String? = null
	
	}
	
}