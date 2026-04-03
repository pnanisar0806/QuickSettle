package com.quicksettle.domain.model

data class SplitSession(
    val description: String,
    val totalAmount: Double,
    val participants: List<ParticipantSplit>,
)
