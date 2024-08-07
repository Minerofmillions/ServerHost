package io.github.minerofmillions.utils

fun String.truncate(chars: Int, truncationIndicator: String = "...") =
    if (length <= chars) this else take(chars) + truncationIndicator

fun String.splitCommand() = split(' ').let {
    var currentWord = ""
    var quotesOpen = false
    buildList {
        for (string in it) {
            if (string.startsWith('"')) quotesOpen = true
            if (quotesOpen) {
                currentWord += string
                if (currentWord.endsWith('"')) {
                    quotesOpen = false
                    add(currentWord)
                }
            } else add(string)
        }
    }
}