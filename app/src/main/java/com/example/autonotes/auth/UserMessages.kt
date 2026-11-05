package com.example.autonotes.auth

fun Throwable.toUserMessage(): String {
    val text = (message ?: "").lowercase()
    return when {
        "invalid login credentials" in text -> "Неверная почта или пароль"
        "already registered" in text || "already been registered" in text ->
            "Эта почта уже зарегистрирована"
        "password should be at least" in text || "weak_password" in text ->
            "Пароль слишком короткий (не менее 8 символов)"
        "unable to validate email" in text || "invalid email" in text ->
            "Некорректный адрес почты"
        "rate limit" in text || "too many requests" in text ->
            "Слишком много попыток, подождите минуту"
        "unable to resolve host" in text || "timeout" in text ||
                "failed to connect" in text || "network" in text ->
            "Нет соединения с интернетом"
        else -> "Ошибка: ${message ?: "неизвестная"}"
    }
}