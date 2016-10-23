package br.com.kodeless.minimaljs;

import java.util.Date;

import br.com.kodeless.minimaljs.dao.XDAO;
import br.com.kodeless.minimaljs.model.internal.XScheduledExecution;

public class XScheduleService {

	XDAO<XScheduledExecution> dao = new XDAO<XScheduledExecution>(XScheduledExecution.class);
	
	public void executeOnDate(String methodName, Date date){
		XScheduledExecution exec = new XScheduledExecution();
		exec.setExecuted(false);
		exec.setExecutionDate(date);
		exec.setScheduleName(methodName);
		dao.insert(exec);
	}
}
