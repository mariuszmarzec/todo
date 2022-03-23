package com.marzec.extensions

fun <T> List<T>.replaceIf(condition: (T) -> Boolean, replace: (T) -> T): List<T> = map { item: T ->
    if (condition(item)) {
        replace(item)
    } else {
        item
    }
}

fun <T> List<T>.filterWithSearch(
    search: String,
    stringsToCompare: (T) -> List<String>
): List<T> {
    val searchQuery = search.trim().split(" ")
    return filter { item ->
        searchQuery == listOf("") || searchQuery.all { searchPart ->
            stringsToCompare(item).any { text ->
                text.contains(
                    searchPart,
                    ignoreCase = true
                )
            }
        }
    }
}
