package dev.mitin.internal

internal fun validAuthForm(mode:String,email:String,name:String,password:String,confirmation:String,token:String,consent:Boolean,terms:Boolean,phone:String=""):Boolean {
    val emailValid = email.trim().length in 3..254 && Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim())
    val passwordValid = passwordError(password) == null
    val tokenValid = Regex("[A-Za-z0-9_-]{43}").matches(token)
    return when(mode) {
        "login" -> emailValid && passwordValid
        "register" -> emailValid && name.trim().codePointCount(0,name.trim().length) in 1..120 && passwordValid && password==confirmation && consent && terms && phoneError(phone) == null
        "password-reset/request", "resend-verification" -> emailValid
        "verify-email" -> tokenValid
        "password-reset/confirm" -> tokenValid && passwordValid && password==confirmation
        else -> false
    }
}

internal fun passwordError(value: String): String? = when {
    value.codePointCount(0, value.length) < 6 -> "Пароль слишком короткий: нужно минимум 6 символов."
    value.codePointCount(0, value.length) > 128 -> "Пароль должен содержать не больше 128 символов."
    !Charsets.UTF_8.newEncoder().canEncode(value) -> "Пароль содержит недопустимый символ."
    else -> null
}

internal fun phoneError(value: String): String? {
    val phone = value.trim()
    if (phone.isEmpty()) return null
    return if (phone.length <= 40 && Regex("\\+?[0-9() \\-]+").matches(phone) &&
        phone.count { it in '0'..'9' } in 7..15) null
    else "Укажите телефон: 7–15 цифр, можно использовать +, пробелы, скобки и дефисы."
}
