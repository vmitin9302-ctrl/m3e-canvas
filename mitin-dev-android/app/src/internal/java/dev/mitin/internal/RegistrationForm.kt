package dev.mitin.internal

internal fun validAuthForm(mode:String,email:String,name:String,password:String,confirmation:String,token:String,consent:Boolean,terms:Boolean):Boolean {
    val emailValid = email.trim().length in 3..254 && Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim())
    val passwordValid = password.codePointCount(0,password.length) in 15..128
    val tokenValid = Regex("[A-Za-z0-9_-]{43}").matches(token)
    return when(mode) {
        "login" -> emailValid && passwordValid
        "register" -> emailValid && name.trim().length in 1..120 && passwordValid && password==confirmation && consent && terms
        "password-reset/request", "resend-verification" -> emailValid
        "verify-email" -> tokenValid
        "password-reset/confirm" -> tokenValid && passwordValid && password==confirmation
        else -> false
    }
}
