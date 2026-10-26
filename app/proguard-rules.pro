# ZXing core keeps reflection-free; default rules suffice. Keep the parsed-result
# classes referenced via ResultParser to be safe under R8 full mode.
-keep class com.google.zxing.client.result.** { *; }
