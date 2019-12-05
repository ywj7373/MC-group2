package com.example.bluecatapp.ui.location

fun getNumberOfASCII(str:String) : Int {
    var index = 0
    var count = 0
    while(index<str.length) {
        if( str.get(index) <= 127.toChar()) {        // if character is ASCII
            count++
        }
        index++
    }
    return count
}

fun getNumberOfNonASCII(str:String) : Int {
    var index = 0
    var count = 0
    while(index<str.length) {
        if( str.get(index) > 127.toChar()) {        // if character is non-ASCII
            count++
        }
        index++
    }
    return count
}

fun getSlicePosition(str:String, num:Int) : Int {
    // Input : original string & length(Hangul is computed 2) of substring you want to get
    // Output : index of position to cut
    var index = 0
    var count = 0
    while(index<str.length) {
        if( str.get(index) <= 127.toChar()) {        // if character is ASCII
            count++
        }
        else {
            count = count + 2
        }
        index++

        if(count >= num)
            return index
    }
    return -1
}

fun repeat(count: Int, with: String): String {
    return String(CharArray(count)).replace("\u0000", with)
}
