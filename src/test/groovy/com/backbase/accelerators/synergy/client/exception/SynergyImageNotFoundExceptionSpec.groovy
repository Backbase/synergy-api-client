package com.backbase.accelerators.synergy.client.exception

import spock.lang.Specification

class SynergyImageNotFoundExceptionSpec extends Specification {

    void 'assert synergyImageNotFoundException message is set'() {
        given:
        SynergyImageNotFoundException exception = new SynergyImageNotFoundException('fake exception')

        expect:
        exception.message == 'fake exception'
    }
}
