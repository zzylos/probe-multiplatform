package org.ooni.probe.domain.descriptors

import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.toModel
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.di.Dependencies
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the descriptors.json bundled with each organization, read from disk so both are
 * covered no matter which organization the tests are built for.
 */
class BundledDescriptorsTest {
    private val json = Dependencies.buildJson()

    @Test
    fun descriptorsHaveUniqueIds() {
        ORGANIZATIONS.forEach { organization ->
            val ids = readDescriptors(organization).map { it.id }
            assertTrue(ids.isNotEmpty(), "No descriptors for $organization")
            assertEquals(ids.size, ids.toSet().size, "Duplicated descriptor IDs for $organization")
        }
    }

    @Test
    fun localDescriptorsOnlyUseSupportedTests() {
        localDescriptors().forEach { descriptor ->
            descriptor.netTests.forEach { netTest ->
                assertTrue(
                    SUPPORTED_LOCAL_TESTS.containsKey(netTest.test.name),
                    "${descriptor.id.value} uses unsupported test ${netTest.test.name}",
                )
            }
        }
    }

    @Test
    fun localDescriptorInputsMatchTheirTests() {
        localDescriptors().forEach { descriptor ->
            descriptor.netTests.forEach { netTest ->
                val inputs = netTest.inputs.orEmpty()
                val inputFormat = SUPPORTED_LOCAL_TESTS[netTest.test.name]
                if (inputFormat == null) {
                    assertTrue(
                        inputs.isEmpty(),
                        "${descriptor.id.value} ${netTest.test.name} should use the engine default inputs",
                    )
                } else {
                    assertTrue(
                        inputs.isNotEmpty(),
                        "${descriptor.id.value} ${netTest.test.name} requires inputs",
                    )
                    inputs.forEach { input ->
                        assertTrue(
                            inputFormat.matches(input),
                            "${descriptor.id.value} ${netTest.test.name} has invalid input $input",
                        )
                    }
                    assertEquals(
                        inputs.size,
                        inputs.toSet().size,
                        "${descriptor.id.value} ${netTest.test.name} has duplicated inputs",
                    )
                }
            }
        }
    }

    @Test
    fun localDescriptorsRunManuallyByDefault() {
        localDescriptors().forEach { descriptor ->
            descriptor.netTests.forEach { netTest ->
                assertTrue(
                    netTest.isManualRunEnabled,
                    "${descriptor.id.value} ${netTest.test.name} is disabled for manual runs",
                )
            }
        }
    }

    @Test
    fun ooniHasLocalDescriptors() {
        val ids = readDescriptors("ooniMain").filter { it.isLocalDescriptor }.map { it.id.value }
        assertEquals(
            listOf("local-vpns", "local-blocking-methods", "local-social-media", "local-news-media"),
            ids,
        )
    }

    private fun localDescriptors() =
        ORGANIZATIONS
            .flatMap(::readDescriptors)
            .filter { it.isLocalDescriptor }

    private fun readDescriptors(organization: String): List<Descriptor> {
        val file = File("src/$organization/composeResources/files/assets/descriptors.json")
        assertTrue(file.exists(), "Missing ${file.absolutePath}")
        val descriptors = try {
            json.decodeFromString<List<OONIRunDescriptor>>(file.readText())
        } catch (e: Exception) {
            fail("Could not parse ${file.path}", e)
        }
        return descriptors.map { it.toModel() }
    }

    companion object {
        private val ORGANIZATIONS = listOf("ooniMain", "dwMain")

        private const val HOST = "[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+"

        /**
         * Tests a local descriptor can use, and the format of their inputs (null if the engine
         * provides the inputs itself). Other experiments in the engine are left out on purpose:
         * openvpn, torsf and vanilla_tor are disabled by the check-in API, riseupvpn is disabled
         * by default in the engine, and portfiltering has no test helper yet.
         */
        private val SUPPORTED_LOCAL_TESTS: Map<String, Regex?> = mapOf(
            "web_connectivity" to Regex("^https?://$HOST/\\S*$"),
            "tlsping" to Regex("^tlshandshake://$HOST:\\d+$"),
            "tcpping" to Regex("^tcpconnect://$HOST:\\d+$"),
            "quicping" to Regex("^$HOST$"),
            "sniblocking" to Regex("^$HOST$"),
            "httphostheader" to Regex("^$HOST$"),
            "dnscheck" to null,
            "echcheck" to null,
            "stunreachability" to null,
        )
    }
}
