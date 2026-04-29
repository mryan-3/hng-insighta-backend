package com.hng.stage3.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;

import java.util.UUID;

public class UuidUtils {
    private static final NoArgGenerator generator = Generators.timeBasedEpochGenerator();

    public static UUID generateV7() {
        return generator.generate();
    }
}
