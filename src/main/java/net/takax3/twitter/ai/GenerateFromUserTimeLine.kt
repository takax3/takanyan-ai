package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import twitter4j.Paging
import twitter4j.Twitter
import java.io.*
import kotlin.concurrent.thread


object GenerateFromUserTimeLine {
	
	private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
	private var twitter: Twitter? = null
	
	private var config: Config? = null
	
	@JvmStatic
	fun main(args: Array<String>) {
		
		twitter = TwitterConnector.connect()
		
		File("Config_UTL.json").run {
			if (!exists()) {
				try {
					config = Config()
					FileWriter(this).run {
						write(gson.toJson(config))
						close()
					}
				} catch (e: Exception) {
					e.printStackTrace()
					return
				}
			} else {
				try {
					FileReader(this).run {
						config = gson.fromJson(this, Config::class.java)
						close()
					}
				} catch (e: Exception) {
					e.printStackTrace()
					return
				}
			}
		}
		
		while (true) {
			
			thread {
				val sourceList = ArrayList<String>()
				
				// 解析元データ取得
				val paging = Paging()
				paging.count = 200
				
				val maxTweets = if (config!!.collectTweetNum!! < twitter!!.showUser(config!!.ownerUserID!!).statusesCount) config!!.collectTweetNum!! else twitter!!.showUser(config!!.ownerUserID!!).statusesCount
				
				for (i in 0..maxTweets / 200) {
					if (i == maxTweets / 200) {
						if (maxTweets % 200 == 0) break else paging.count = maxTweets % 200
					}
					twitter!!.getUserTimeline(config!!.ownerUserID!!, paging).run {
						for (status in this) {
							// 一件一件の解析
							status.text.run {
								// リプライ、URL付、タグツイを除外
								if (!WordPreProcessor.isIncludeExcludeWords(this) && status.user.id != twitter!!.id) {
									println("------------------------")
									println(this)
									sourceList.add(this)
								}
							}
							paging.maxId = status.id
						}
					}
					
				}
				
				val analyzedJsonObject = Analyzer().analyze(sourceList)
				
				println(gson.toJson(analyzedJsonObject))
				var outputText: String
				do outputText = Analyzer().generate(analyzedJsonObject) while (outputText.length > 140)
				
				// 出力
				println(outputText)
				twitter!!.updateStatus(outputText)
			}
			
			Thread.sleep((config!!.intervalMinute!! * 60 * 1000).toLong())
			
		}
		
	}
	
	
	class Config {
		
		var ownerUserID: String? = "takax3_M"
		var intervalMinute: Int? = 15
		var collectTweetNum: Int? = 1000
		
	}
	
}