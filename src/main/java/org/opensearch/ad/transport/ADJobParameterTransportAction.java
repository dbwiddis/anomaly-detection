package org.opensearch.ad.transport;

import static org.opensearch.ad.util.RestHandlerUtils.wrapRestActionListener;
import static org.opensearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.ad.model.AnomalyDetectorJob;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.extensions.action.ExtensionActionRequest;
import org.opensearch.extensions.action.ExtensionActionResponse;
import org.opensearch.jobscheduler.model.ExtensionJobParameter;
import org.opensearch.jobscheduler.spi.ScheduledJobParameter;
import org.opensearch.jobscheduler.transport.request.JobParameterRequest;
import org.opensearch.jobscheduler.transport.response.ExtensionJobActionResponse;
import org.opensearch.jobscheduler.transport.response.JobParameterResponse;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport Action to parse Anomaly Detector
 */
public class ADJobParameterTransportAction extends HandledTransportAction<ExtensionActionRequest, ExtensionActionResponse> {

    private static final Logger LOG = LogManager.getLogger(ADJobParameterTransportAction.class);

    private final NamedXContentRegistry xContentRegistry;

    protected ADJobParameterTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        NamedXContentRegistry xContentRegistry
    ) {
        super(ADJobParameterAction.NAME, transportService, actionFilters, ExtensionActionRequest::new);
        this.xContentRegistry = xContentRegistry;
    }

    @Override
    protected void doExecute(Task task, ExtensionActionRequest request, ActionListener<ExtensionActionResponse> actionListener) {

        String errorMessage = "Failed to parse the Job Parameter";
        ActionListener<ExtensionActionResponse> listener = wrapRestActionListener(actionListener, errorMessage);
        JobParameterRequest jobParameterRequest;
        try {
            jobParameterRequest = new JobParameterRequest(request.getRequestBytes());
            XContentParser parser = XContentHelper
                .createParser(xContentRegistry, LoggingDeprecationHandler.INSTANCE, jobParameterRequest.getJobSource(), XContentType.JSON);
            ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);
            ScheduledJobParameter scheduledJobParameter = AnomalyDetectorJob.parse(parser);
            JobParameterResponse jobParameterResponse = new JobParameterResponse(new ExtensionJobParameter(scheduledJobParameter));
            listener.onResponse(new ExtensionJobActionResponse<>(jobParameterResponse));
        } catch (Exception e) {
            LOG.error(e);
            listener.onFailure(e);
        }
    }
}