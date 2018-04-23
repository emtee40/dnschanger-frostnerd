package com.frostnerd.dnschanger.util.dnsquery;

import java.util.Collections;
import java.util.Set;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.MiniDNSException;
import de.measite.minidns.Question;
import de.measite.minidns.dnssec.DNSSECResultNotAuthenticException;
import de.measite.minidns.dnssec.UnverifiedReason;
import de.measite.minidns.hla.ResolutionUnsuccessfulException;
import de.measite.minidns.record.Data;

/*
  Modified by Daniel Wolf (frostnerd.com)
  Original author: https://github.com/MiniDNS/minidns

  Licensed under the WTFPL
 */
public class ResolverResult<D extends Data> {
    private final Question question;
    private final DNSMessage.RESPONSE_CODE responseCode;
    private final Set<D> data;
    private final boolean isAuthenticData;
    private final Set<UnverifiedReason> unverifiedReasons;
    private final DNSMessage dnsMessage;

    ResolverResult(Question question , DNSMessage answer, Set<UnverifiedReason> unverifiedReasons) throws MiniDNSException.NullResultException {
        if (answer == null) {
            throw new MiniDNSException.NullResultException(question.asMessageBuilder().build());
        }
        this.dnsMessage = answer;
        this.question = question;
        this.responseCode = answer.responseCode;

        Set<D> r = answer.getAnswersFor(question);
        if (r == null) {
            this.data = Collections.emptySet();
        } else {
            this.data = Collections.unmodifiableSet(r);
        }

        if (unverifiedReasons == null) {
            this.unverifiedReasons = null;
            isAuthenticData = false;
        } else {
            this.unverifiedReasons = Collections.unmodifiableSet(unverifiedReasons);
            isAuthenticData = this.unverifiedReasons.isEmpty();
        }
    }

    public DNSMessage getDnsMessage() {
        return dnsMessage;
    }

    public boolean wasSuccessful() {
        return responseCode == DNSMessage.RESPONSE_CODE.NO_ERROR;
    }

    public Set<D> getAnswers() {
        throwIseIfErrorResponse();
        return data;
    }

    public Set<D> getAnswersOrEmptySet() {
        return data;
    }

    public DNSMessage.RESPONSE_CODE getResponseCode() {
        return responseCode;
    }

    public boolean isAuthenticData() {
        throwIseIfErrorResponse();
        return isAuthenticData;
    }

    public Set<UnverifiedReason> getUnverifiedReasons() {
        throwIseIfErrorResponse();
        return unverifiedReasons;
    }

    public Question getQuestion() {
        return question;
    }

    public void throwIfErrorResponse() throws ResolutionUnsuccessfulException {
        ResolutionUnsuccessfulException resolutionUnsuccessfulException = getResolutionUnsuccessfulException();
        if (resolutionUnsuccessfulException != null) throw resolutionUnsuccessfulException;
    }

    private ResolutionUnsuccessfulException resolutionUnsuccessfulException;

    public ResolutionUnsuccessfulException getResolutionUnsuccessfulException() {
        if (wasSuccessful()) return null;

        if (resolutionUnsuccessfulException == null) {
            resolutionUnsuccessfulException = new ResolutionUnsuccessfulException(question, responseCode);
        }

        return resolutionUnsuccessfulException;
    }

    private DNSSECResultNotAuthenticException dnssecResultNotAuthenticException;

    public DNSSECResultNotAuthenticException getDnssecResultNotAuthenticException() {
        if (!wasSuccessful())
            return null;
        if (isAuthenticData)
            return null;

        if (dnssecResultNotAuthenticException == null) {
            dnssecResultNotAuthenticException = DNSSECResultNotAuthenticException.from(getUnverifiedReasons());
        }

        return dnssecResultNotAuthenticException;
    }

    private void throwIseIfErrorResponse() {
        ResolutionUnsuccessfulException resolutionUnsuccessfulException = getResolutionUnsuccessfulException();
        if (resolutionUnsuccessfulException != null)
            throw new IllegalStateException("Can not perform operation because the DNS resolution was unsuccessful",
                    resolutionUnsuccessfulException);
    }
}
