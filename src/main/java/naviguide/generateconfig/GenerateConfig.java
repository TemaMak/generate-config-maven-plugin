package naviguide.generateconfig;

import org.apache.maven.model.Profile;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goal which touches a timestamp file.
 *
 */
@Mojo( name = "config-generate", defaultPhase = LifecyclePhase.COMPILE )
public class GenerateConfig   extends AbstractMojo {
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> projects;        
    
    private String configName = "config";
    
    protected List<String> templates = new ArrayList<String>();
    
    public void execute() throws MojoExecutionException {
       
    	Log logger = getLog();
    	logger.info("hello from gen config plugin");
    	
    	File f = outputDirectory;

        if ( !f.exists() ) {
            f.mkdirs();
        }
    	
    	logger.info("read config template");
    	
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader("./src/main/resources/config-template/config.template"));
			String line = reader.readLine();
			while (line != null) {
				logger.info("read line from file: " + line);
				templates.add(line);
				// read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
        
    	for(MavenProject project: projects){
    		logger.info("find project: " + project.getId());	
        	for(Profile p: project.getModel().getProfiles()){
        		logger.info(" => find profile: " + p.getId());
        		processProfile(p);        		
        	}
    	}
    }
    
    protected void processProfile(Profile p) throws MojoExecutionException{
    	Log logger = getLog();
    	logger.info("process profile: " + p.toString());
    	
    	try {
			logger.info("try create file in " + outputDirectory.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	logger.info("read original profile properties");
    	String propertyProfileName = "profiles/" + p.getId() + "/config.properties";
    	Properties prop = new Properties();
    	InputStream input = null;
    			
    	try {
			input = new FileInputStream(propertyProfileName);
			prop.load(input);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
    	  	    	
    	String profileFileName = configName + "." + p.getId(); 
    	File profileConfig = new File( outputDirectory, profileFileName );
    	
        FileWriter w = null;
        String modifyString = null;
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        
        try {
            w = new FileWriter( profileConfig );
            
            for(String s: templates){
            	modifyString = s;
            	Matcher matcher = pattern.matcher(modifyString);
            	
                while (matcher.find()) {
                	logger.info("find variable: " + matcher.group(1));
                	
                	logger.info("load variable value: " + prop.getProperty(matcher.group(1)));
                	
                	modifyString = modifyString.replaceFirst(
            			"\\$\\{" + matcher.group(1) + "\\}", 
            			prop.getProperty(matcher.group(1))
        			);

                }
            	
            	logger.info("[" + s + "] => [" + modifyString + "]");
            	w.write(modifyString + "\n");
            }                        
        }catch ( IOException e ) {
            throw new MojoExecutionException( "Error creating file " + profileConfig, e );
        } finally {
            if ( w != null ) {
                try {
                    w.close();
                } catch ( IOException e ) {
                    // ignore
                }
            }
        }    	
    }
}
