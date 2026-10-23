package com.github.vinisuxz.ggclient.errors

class CommandError(message: String, cause: Throwable) : Error(message, cause)