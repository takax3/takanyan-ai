package net.takax3.twitter.ai

import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI

class OAuth {
	
	fun getAccessToken(consumerKey: String, consumerSecret: String): AccessToken? {
		
		var accessToken: AccessToken? = null
		
		try {
			println("Oauth認証を行います")
			val br = BufferedReader(InputStreamReader(System.`in`))
			while (null == accessToken) {
				val twitter = TwitterFactory().instance
				twitter.setOAuthConsumer(consumerKey, consumerSecret)
				val requestToken = twitter.oAuthRequestToken
				java.awt.Desktop.getDesktop().browse(URI(requestToken.authenticationURL))
				print("認証後に表示されるPINコードを入力してください... : ")
				val pin = br.readLine()
				accessToken = try {
					if (pin.isNotEmpty()) {
						twitter.getOAuthAccessToken(requestToken, pin)
					} else {
						twitter.oAuthAccessToken
					}
				} catch (te: TwitterException) {
					null
				}
				
			}
			return accessToken
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		}
		
	}
	
}