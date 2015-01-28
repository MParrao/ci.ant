/**
 * (C) Copyright IBM Corporation 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.ant;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;

/**
 * Uninstall feature task.
 */
public class UninstallFeatureTask extends AbstractTask {

    private String cmd;
    private String cmdServer;

    // Skip user confirmation and uninstall the feature
    private boolean noPrompts = false;

    // name of the feature to uninstall or URL
    private String name;

    @Override
    protected void initTask() {
        super.initTask();

        if (isWindows) {
            cmd = installDir + "\\bin\\featureManager.bat";
            cmdServer = installDir + "\\bin\\server.bat";
            processBuilder.environment().put("EXIT_ALL", "1");
        } else {
            cmd = installDir + "/bin/featureManager";
            cmdServer = installDir + "/bin/server";
        }

        Properties sysp = System.getProperties();
        String javaHome = sysp.getProperty("java.home");

        // Set active directory (install dir)
        processBuilder.directory(installDir);
        processBuilder.environment().put("JAVA_HOME", javaHome);

    }

    @Override
    public void execute() {
        if (name == null || name.length() <= 0) {
            throw new BuildException(MessageFormat.format(messages.getString("error.server.operation.validate"), "name"));
        }
        
        initTask();
        
        try {
            if (!checkValidVersion()) {
                throw new BuildException("You can't uninstall a feature because Liberty version doesn't supported");
            }
            doUninstall();
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void doUninstall() throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(cmd);
        command.add("uninstall");      
        if (noPrompts) {
            command.add("--noPrompts");
        }
        command.add(name);
        processBuilder.command(command);
        Process p = processBuilder.start();
        checkReturnCode(p, processBuilder.command().toString(), ReturnCode.OK.getValue());
    }

    private boolean checkValidVersion() throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(cmdServer);
        command.add("version");

        processBuilder.command(command);
        Process p = processBuilder.start();
        checkReturnCode(p, processBuilder.command().toString(), ReturnCode.OK.getValue());

        Pattern pattern = null;
        pattern = Pattern.compile("^((\\w*\\s)+)(?<major>\\d+)(\\.(?<minor>\\d+))?(\\.(?<micro>\\d+))?(\\.(?<qualifier>(.)+))?(\\s\\(\\d)(.)+$");
        Matcher m = pattern.matcher(result);


        if (m.matches()) {
            int major = parseComponent(m.group("major"));
            int minor = parseComponent(m.group("minor"));
            int micro = parseComponent(m.group("micro"));
            int qualifier;
            try{
               qualifier = parseComponent(m.group("qualifier"));
            } catch(NumberFormatException e) {
                qualifier=0;
            }

            if (major >= 2015) {
                if (major < 2015){
                    return false;
                } else if (minor < 4) {
                    return false;
                } else if (micro < 0) {
                    return false;
                } else if (qualifier < 0) {
                    return false;
                } else {
                    return true;
                }
            }

            if (major < 8) {
                return false;
            } else if (minor < 5) {
                return false;
            } else if (micro < 5) {
                return false;
            } else if (qualifier < 5) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }

    }

    private static int parseComponent(String version) {
        if (version == null) {
            return 0;
        }  else {
            return Integer.parseInt(version);
        }
    }

    /**
     * @return quiet value
     */
    public boolean isNoPrompts() {
        return noPrompts;
    }

    /**
     * @param acceptLicense the acceptLicense to set
     */
    public void setNoPrompts(boolean noPrompts) {
        this.noPrompts = noPrompts;
    }

    /**
     * @return the feature name
     */
    public String getName() {
        return name;
    }

    /**
     * @param the feature name
     */
    public void setName(String name) {
        this.name = name;
    }

}
