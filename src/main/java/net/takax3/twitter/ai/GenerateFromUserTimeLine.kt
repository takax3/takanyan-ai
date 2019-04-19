package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.io.*
import kotlin.collections.ArrayList


object GenerateFromUserTimeLine {
	
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
		
		accessToken = ReadAccessTokens.read(consumerKey, consumerSecret)
		if (accessToken !is AccessToken) return
		
		twitter = TwitterFactory().instance
		twitter!!.setOAuthConsumer(consumerKey, consumerSecret)
		twitter!!.oAuthAccessToken = accessToken
		
		val configFile = File("Config_UTL.json")
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
			
			val sourceList = ArrayList<String>()
			
			// 解析元データ取得
			val paging = Paging()
			paging.count = 200
			
			val maxTweets = if (config!!.collectTweetNum!! < twitter!!.showUser(config!!.ownerUserID!!).statusesCount) config!!.collectTweetNum!! else twitter!!.showUser(config!!.ownerUserID!!).statusesCount
			
			for (i in 0 .. maxTweets / 200) {
				if (i == maxTweets / 200) {
					if (maxTweets % 200 == 0) break else paging.count = maxTweets % 200
				}
				val timeline = twitter!!.getUserTimeline(config!!.ownerUserID!!, paging)
				
				// 一件一件の解析
				for (status in timeline) {
					val text = status.text
					// リプライ、URL付、タグツイを除外
					if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1 && status.user.id != twitter!!.id) {
						
						println("------------------------")
						println(status.text)
						sourceList.add(status.text)
						
					}
					paging.maxId = status.id
				}
				
			}
			
			val analyzedJsonObject = Analyzer().analyze(sourceList)
			println(gson.toJson(analyzedJsonObject))
			var outputText: String
			do outputText = Analyzer().generate(analyzedJsonObject) while (outputText.length > 140)
			
			// 出力
			println(outputText)
			twitter!!.updateStatus(outputText)
			
			Thread.sleep((config!!.intervalMinute!! * 60 * 1000).toLong())
			
		}
		
	}
	
	
	class Config {
		
		var ownerUserID: String? = "takax3_M"
		var intervalMinute: Int? = 15
		var collectTweetNum: Int? = 1000
		
	}
	
}