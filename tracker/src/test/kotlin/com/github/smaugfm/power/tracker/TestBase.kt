package com.github.smaugfm.power.tracker

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(classes = [TrackerApplication::class])
class TestBase {
}
