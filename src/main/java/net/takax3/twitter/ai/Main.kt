package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer
import java.io.*
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList


object Main {
	
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
		
		val paging = Paging()
		paging.count = 200
		val timeline = twitter!!.getHomeTimeline(paging)
		
		for (status in timeline) {
			val text = status.text
			if (text.indexOf("@") == -1 && text.indexOf("://") == -1 && text.indexOf("#") == -1) {
				println("------------------------")
				println(status.text)
				println(split(text))
			}
		}
		
	}
	
	
	private fun split(string: String) : List<String>? {
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