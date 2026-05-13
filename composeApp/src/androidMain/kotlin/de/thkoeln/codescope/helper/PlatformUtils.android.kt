package de.thkoeln.codescope.helper

import dev.gitlive.firebase.storage.Data

fun ByteArray.toData(): Data = Data(this)
