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
						sourceList.add(status.text)
						
					}
					paging.maxId = status.id
				}
				
			}
			
			val analyzedJsonObject = Analyzer().analyze(sourceList)
			println(gson.toJson(analyzedJsonObject))
			val outputText = Analyzer().generate(analyzedJsonObject)
			
			// 出力
			println(outputText)
			twitter!!.updateStatus(outputText)
			
			Thread.sleep((config!!.intervalMinute!! * 60 * 1000).toLong())
			
		}
		
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