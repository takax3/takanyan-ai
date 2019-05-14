package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.opencsv.bean.CsvToBeanBuilder
import twitter4j.Twitter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.concurrent.thread

object GenerateFromCSV {
	
	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	private var twitter: Twitter? = null
	
	@JvmStatic
	fun main(args: Array<String>) {
		
		twitter = TwitterConnector.connect()
		
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
				tweet.text!!.run {
					if (contains("@") && contains("://") && contains("#") && tweet.source!!.contains("UserReport")) {
						println("------------------------")
						println(this)
						source.add(this)
					}
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
			thread {
				var outputText: String
				do outputText = Analyzer().generate(analyzedWords) while (outputText.length > 140)
				
				twitter!!.updateStatus("$outputText\r\n(Source Full Ver)")
				println("$outputText\r\n(Source Full Ver)")
			}
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