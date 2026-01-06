package com.tomclaw.imageloader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class HandlersTest {

    @Test
    fun `default handlers do nothing`() {
        val handlers = Handlers<Any>()
        val viewHolder = mock<ViewHolder<Any>>()
        val result = mock<Result>()

        // Should not throw
        handlers.placeholder.invoke(viewHolder)
        handlers.success.invoke(viewHolder, result)
        handlers.error.invoke(viewHolder)
    }

    @Test
    fun `placeholderHandler sets placeholder callback`() {
        var called = false
        val handlers = Handlers<Any>()
            .placeholderHandler { called = true }

        handlers.placeholder.invoke(mock())

        assertTrue(called)
    }

    @Test
    fun `errorHandler sets error callback`() {
        var called = false
        val handlers = Handlers<Any>()
            .errorHandler { called = true }

        handlers.error.invoke(mock())

        assertTrue(called)
    }

    @Test
    fun `successHandler sets success callback`() {
        var receivedResult: Result? = null
        val expectedResult = mock<Result>()
        val handlers = Handlers<Any>()
            .successHandler { _, result -> receivedResult = result }

        handlers.success.invoke(mock(), expectedResult)

        assertEquals(expectedResult, receivedResult)
    }

    @Test
    fun `handlers can be chained`() {
        var placeholderCalled = false
        var errorCalled = false
        var successCalled = false

        val handlers = Handlers<Any>()
            .placeholderHandler { placeholderCalled = true }
            .errorHandler { errorCalled = true }
            .successHandler { _, _ -> successCalled = true }

        handlers.placeholder.invoke(mock())
        handlers.error.invoke(mock())
        handlers.success.invoke(mock(), mock())

        assertTrue(placeholderCalled)
        assertTrue(errorCalled)
        assertTrue(successCalled)
    }

    @Test
    fun `successHandler composes multiple handlers`() {
        val callOrder = mutableListOf<Int>()

        val handlers = Handlers<Any>()
            .successHandler { _, _ -> callOrder.add(1) }
            .successHandler { _, _ -> callOrder.add(2) }
            .successHandler { _, _ -> callOrder.add(3) }

        handlers.success.invoke(mock(), mock())

        assertEquals(listOf(1, 2, 3), callOrder)
    }

    @Test
    fun `placeholderHandler composes multiple handlers`() {
        val callOrder = mutableListOf<Int>()

        val handlers = Handlers<Any>()
            .placeholderHandler { callOrder.add(1) }
            .placeholderHandler { callOrder.add(2) }

        handlers.placeholder.invoke(mock())

        assertEquals(listOf(1, 2), callOrder)
    }

    @Test
    fun `errorHandler composes multiple handlers`() {
        val callOrder = mutableListOf<Int>()

        val handlers = Handlers<Any>()
            .errorHandler { callOrder.add(1) }
            .errorHandler { callOrder.add(2) }

        handlers.error.invoke(mock())

        assertEquals(listOf(1, 2), callOrder)
    }
}

