# I8n (Internationalisation) Support

A map which can be used to access localized messages bundled as resources on the class path.

This map which behaves  in every sense of the word like a normal (_read only_) `Map<String,String?` Instance, but with some special powers:

1. Accessing  lower level errors on the instance via the special default invoke function  (for example): 

   ```kotlin
   val (message, failure) = messages(key)
   
   failure?.apply {
     when (error) {
       is I8nError.MissingResourceBundle -> TODO()
       is I8nError.MissingMessageKey -> TODO()
       is I8nError.MessageBuildFailure -> TODO()
     }
   }
   ```

2. Threating the localized messages as message templates - enabling you to build enriched user facing messages via the family of `build(..)` functions:

   ```kotlin
   println(messages["user.greeting"])
   // prints: Hello {{ user.name }}! I see you joind on {{ user.joinedDate }}.
   
   // Passing a "bean" to set message values: 
   val m = messages.build("user.greeting", user) 
   // produces: "Hello John! I see you joined on 2020-06-08."
   
   // Passing in a map where each key corresponds message parameter:
   val m = messages.build("user.greeting", mapOf(
     "user.name" to "John",
     "user.joineDdate" to LocalDate.Of(2020, Month.JUNE, 8)))
   
   // Passing a variable list of pairs:
   val m = messages.build("user.greeting", 
       "user.name" to "John",
       "user.joinedDate" to LocalDate.Of(2020, Month.JUNE, 8))
   ```

3. Check if the underling internationalized bundle:

   ```kotlin
   // Does it exist?
   if (messages.bundle.isAvailable()) {
     // Do something with them
   }
   
   // But is empty?
   if (messages.bundle.exists()) {
     // Do something here
   }
   
   // We're accessing this locale: 
   println(messages.bundle.locale)
   
   ```
   
   
   
   

