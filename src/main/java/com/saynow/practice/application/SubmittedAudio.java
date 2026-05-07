package com.saynow.practice.application;

public record SubmittedAudio(
        String filename,
        String contentType,
        long size,
        byte[] content
) {
}
