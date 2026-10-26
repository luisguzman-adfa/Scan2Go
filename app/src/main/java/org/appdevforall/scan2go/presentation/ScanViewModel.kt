package org.appdevforall.scan2go.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.appdevforall.scan2go.domain.ContentParser
import org.appdevforall.scan2go.domain.ScannedContent

/**
 * The single owner of scan state, kept across configuration changes (rotation).
 * Scanning -> Result is one-way per scan: onDecoded is ignored unless we are
 * actively scanning, so a decode landing behind the result sheet cannot fire twice.
 */
class ScanViewModel : ViewModel() {

    sealed interface State {
        data object Scanning : State
        data class Result(val content: ScannedContent) : State
    }

    private val _state = MutableLiveData<State>(State.Scanning)
    val state: LiveData<State> = _state

    var torchOn: Boolean = false
        private set

    fun onDecoded(rawText: String) {
        if (_state.value is State.Scanning) {
            _state.value = State.Result(ContentParser.parse(rawText))
        }
    }

    fun scanAgain() {
        _state.value = State.Scanning
    }

    fun setTorch(on: Boolean) {
        torchOn = on
    }
}
