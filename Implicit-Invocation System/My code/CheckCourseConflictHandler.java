/**
 * @(#)RegisterStudentHandler.java
 *
 * Copyright: Copyright (c) 2003,2004 Carnegie Mellon University
 *
 */

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;


/**
 Check the course is conflict or not, before the student registers.
 */
public class CheckCourseConflictHandler implements Observer {

    boolean isConflict = false;
    protected DataBase objDataBase;

    /**
     * Construct "CheckCourseConflictHandler" Observer
     *
     * @param objDataBase reference to the database object
     */
    public CheckCourseConflictHandler(DataBase objDataBase) {

        EventBus.subscribeTo(EventBus.EV_CHECK_COURSE_CONFLICT, this);
        this.objDataBase = objDataBase;
    }

    /**
     * Process ""CheckCourseConflictHandler" command event.
     *
     * @param param a string parameter for command
     * @return a string result of command processing
     */
    protected boolean execute(String param) {
        // Parse the parameters.
        StringTokenizer objTokenizer = new StringTokenizer(param);
        String sSID     = objTokenizer.nextToken();
        String sCID     = objTokenizer.nextToken();
        String sSection = objTokenizer.nextToken();

        // Get the student and course records.
        Student objStudent = this.objDataBase.getStudentRecord(sSID);
        Course objCourse = this.objDataBase.getCourseRecord(sCID, sSection);

        ArrayList vCourse = objStudent.getRegisteredCourses();

        // Check if the given course conflicts with any of the courses the student has registered.
        for (int i=0; i<vCourse.size(); i++) {
            if (((Course) vCourse.get(i)).conflicts(objCourse)) {
                return true;
//                return "Registration conflicts";
            }
        }
        return false;
    }

    public void update(Observable event, Object param) {
        // If it conflicts, then announce a new output event with "Registration conflicts"
        if(this.execute((String) param)){
            EventBus.announce(EventBus.EV_SHOW, "Registration conflicts");
        }
        else{
            // If not, announce EV_REGISTER_STUDENT to register.
            EventBus.announce(EventBus.EV_REGISTER_STUDENT, param.toString());
        }

    }
}