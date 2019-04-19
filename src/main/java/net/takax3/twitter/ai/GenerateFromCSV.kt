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
		
		accessToken = ReadAccessTokens.read(consumerKey, consumerSecret)
		if (accessToken !is AccessToken) return
		
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
		
		val requiredAnalyze: Boolean
		requiredAnalyze = if (!tweetsJSONFile.exists()) {
			println("解析済みファイルが存在しません。")
			true
		} else if (tweetsJSONFile.lastModified() < tweetsCSVFile.lastModified()) {
			println("解析済みファイルが元ファイルより古いです。")
			true
		} else {
			println("解析済みファイルから処理します。")
			false
		}
		
		if (requiredAnalyze) {
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