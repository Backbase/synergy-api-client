package com.backbase.accelerators.synergy.client.exception

import spock.lang.Specification

class SynergySystemExceptionSpec extends Specification {

    void 'assert synergySystemException message is set'() {
        given:
        SynergySystemException exception = new SynergySystemException('fake exception')

        expect:
        exception.message == 'fake exception'
    }
}
