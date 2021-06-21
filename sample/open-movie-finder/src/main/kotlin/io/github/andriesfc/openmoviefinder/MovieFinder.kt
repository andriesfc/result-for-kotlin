package io.github.andriesfc.openmoviefinder

import io.github.andriesfc.kotlin.result.*

import java.io.IOException

fun main() {
   println(result<IOException,Int> { 12.success() })
}