package uet.fit.client.thread.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.ui.controller.BaseController;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.logger.ProgressDTO;
import uet.fit.dto.logger.RapidlyEntry;

import java.util.List;

public class OnReceiveLogListener implements OnReceiveLog {

	private static final boolean SHOW_LOG = false;
	private static final Logger LOGGER = LoggerFactory.getLogger(OnReceiveLog.class);

	@Override
	public void onSucceeded(List<RapidlyEntry> entries) {
		for (RapidlyEntry entry : entries) {
			if (entry instanceof LogDTO) {
				LogDTO log = (LogDTO) entry;

				// display log on GUI
				switch (log.getPosition()) {
					case ENVIRONMENT:
						HomeController.getInstance().log(log.getType(), log.getMsg());
						break;

					case TEST_BUILD:
						TestController.getInstance().logBuild(log.getType(), log.getMsg());
						break;

					case TEST_GENERAL:
						TestController.getInstance().logGeneral(log.getType(), log.getMsg());
						break;
				}

				// display log on terminal & file
				if (SHOW_LOG) {
					switch (log.getType()) {
						case LogDTO.TYPE_ERR:
							LOGGER.error(log.getMsg());
							break;
						case LogDTO.TYPE_INF:
							LOGGER.info(log.getMsg());
							break;
						case LogDTO.TYPE_DEB:
							LOGGER.debug(log.getMsg());
							break;
					}
				}

			} else if (entry instanceof ProgressDTO) {
				ProgressDTO progress = (ProgressDTO) entry;
				BaseController.getInstance().appendNewProgress(progress);
			}
		}
	}

	@Override
	public void onFailed(Throwable error) {
		LOGGER.error(error.getMessage(), error);
	}
}