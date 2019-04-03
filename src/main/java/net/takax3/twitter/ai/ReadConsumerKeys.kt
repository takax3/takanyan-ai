package net.takax3.twitter.ai

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ReadConsumerKeys {
	
	var consumerKey: String? = null
	var consumerSecret: String? = null
	
	fun read(filename: String = "ConsumerKey.json") : Boolean {
		
		val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
		val file = File(filename)
		
		return if (!file.exists()) {
			try {
				
				val temp = ReadConsumerKeys()
				val fileWriter = FileWriter(file)
				fileWriter.write(gson.toJson(temp))
				fileWriter.close()
				println("生成された $filename にCS/CKを書き込んでください")
				false
				
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
			
		} else {
			try {
				
				println("$filename からCS/CKを読み込みます……")
				val fileReader = FileReader(file)
				val temp = gson.fromJson(fileReader, ReadConsumerKeys::class.java)
				fileReader.close()
				consumerKey = temp.consumerKey
				consumerSecret = temp.consumerSecret
				println("$filename からCS/CKを読み込みました。")
				true
				
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}
	
}