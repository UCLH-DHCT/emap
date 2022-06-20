# Emap Core

This service takes messages from a queue and compares this data to the current data in the EMAP database.
Generally, if a message has newer information that is different, then the message will update the database data,
otherwise the message will have no effect. This is important because the HL7 messages can be received out of order.

## Local setup instructions using IntelliJ IDEA

These setup instructions are aimed at developing in [IntelliJ IDEA](https://www.jetbrains.com/idea/), but hopefully should be similar in Eclipse

1. <details>
    <summary>Create a parent directory</summary>
   
    Create a directory where all the repositories, including this one, will be cloned
    e.g.
   
    ```bash
    mkdir ~/projects/EMAP
    ```
</details>

2. <details>
    <summary>Clone repositories</summary>
   
    Emap-Core depends on both [Inform-DB](https://github.com/inform-health-informatics/Inform-DB) and [Emap-Interchange](https://github.com/inform-health-informatics/Emap-Interchange).
    Clone each of them with e.g.

    ```bash
    cd ~/projects/EMAP
    git clone https://github.com/inform-health-informatics/Emap-Core.git
    git clone https://github.com/inform-health-informatics/Emap-Interchange.git
    git clone https://github.com/inform-health-informatics/Inform-DB.git
    ```
</details>

3. <details>
    <summary>Open project in IntelliJ IDEA</summary>
   
    <b>File > New > New Project From existing sources</b> and select the parent directory (e.g. `~/projects/EMAP`). If prompted, choose "Create project from existing sources"
</details>

4. <details>
    <summary>Add Maven projects</summary>
   
    In the project pane on the top left of the IDE, switch to "Project Files" mode, right-click `Emap-Core/pom.xml` and select <b>Add as Maven project</b>.
    Do the same with `Emap-Interchange/pom.xml` and `Inform-DB/pom.xml` - not to be confused with `Inform-DB/inform-db/pom.xml`! 
    If you add something by mistake use "Unlink Maven projects" in the Maven pane, which is the opposite of "Add..."
</details>

5. <details>
    <summary>Allow annotation processing</summary>
   
    Go to <b>File > Settings > and searching for `processor`</b>
    - Check `enable annotation preprocessing`
    - Change the production sources directory to `classes` as below
   
    ![preprocessor](img/annotation_processor.png)
</details>

6. <details>
    <summary>Reload Maven projects</summary>
   
    In the `Maven` pane (which should now have appeared on the top right of the IDE),
    click **Reimport all maven projects** or **Reload**
</details>

7. <details>
    <summary>Add lombok and checkstyle plugins</summary>
   
    Go to <b>File > Settings > search for plugins</b>, search lombok and checkstyle and install them
</details>

8. <details>
    <summary>Setup checkstyle</summary>
   
    To allow checkstyle to be run from the bottom panel of the IDE go to <b>File > settings > search for checkstyle</b>
    - Set the version of checkstyle to the latest version
    - Click on the `+` to add a new checkstyle configuration

    ![checkstyle_setup](img/checkstyle_setup.png)

    - Make a description and select the checkstyle file in `Emap-Core/inform-checker.xml`. When done, tick box to make the new configuration active.
    ![checkstyle](img/checkstyle.png)
</details>


## Running tests

Emap-Core and the other repositories include unit tests in `<repo-name>/src/test/java`.  Run all the tests by

1. <details>
    <summary>Creating a configuration</summary>
   
    - <b>Run > Edit Configurations</b>
    - Click on the `+` at the top left-hand side of the window
      ![new run](img/new_run.png)
    - Select `Junit` from the drop down
        - Set Test kind to `All in package`
        - Set the package to `uk.ac.ucl.rits.inform.datasinks.emapstar`
        - You may also want to set logging level to TRACE for our classes by defining the environmental variable:
          `LOGGING_LEVEL_UK_AC_UCL=TRACE`

</details>

2. <details>
    <summary>Compiling and running</summary>
    
    Go to <b>Run > Run</b>, which should create a window in the bottom pane
    ![tests pass](img/test_pass.png)

    - If this fails to compile, you may need to go to the maven pane on the right-hand side and
      run the Lifecycle `clean` goal for: `Inform Annotations` and `Inform-DB`.
      Then `clean` and then `install` on `Emap Star Schema`
    - After this then select the `Reload All Maven Projects` icon at the top of the same pane as shown below

      ![reload](img/reload_maven.png)

    - You may also need to run `Generate Sources and Update Folders For All Projects`
</details>

Tests can also be run individually by clicking the play button on a class within an IDE editor window.

## Deploying a live version

How to deploy an instance of Emap on the ULCH GAE, to be run on real patient data. [emap-setup](https://github.com/inform-health-informatics/emap-setup)
manages the multiple repositories and configuration files.


1. <details>
    <summary>Create a directory with the correct permissions</summary>

    Find a place to put the source code. If this instance is not attached to a person, a directory in `/gae` is a good place. For example, `/gae/emap-live/`, and this will be the example used in these instructions.
    e.g.
    
    ```bash
    mkdir /gae/emap-live
    chgrp -R docker /gae/emap-live
    chmod -R g+rwx /gae/emap-live
    setfacl -R -m d:g::rwX /gae/emap-live
    ```
    
    to create, modify the group, change ownership (`s` ensures the group ownership but not the permissions are inherited)
    and inherit permissions.
    
    It should now look like e.g.
    
    ```bash
    $ ls -la /gae/emap-live
    total 8
    drwxrws---+  2 jstein01 docker 4096 Oct 16 10:59 .
    drwxrwx---. 22 root     docker 4096 Oct 16 10:52 ..
    ```
   
</details>

2. <details>
    <summary>Set the git configuration</summary>

    Create a [personal access token](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token) 
    for the next step and allow your username and access token to be saved with

    ```shell
    git config --global credential.helper store
    ```

    **Note**: this will allow storage of the connection information in plain text in your home directory. We use https 
    because outgoing ssh is blocked from the GAE.
</details>

3. <details>
    <summary>Install <b>emap-setup</b></summary>
   
    Install the `emap` script by cloning the repository

    ```bash
    git clone https://github.com/inform-health-informatics/emap-setup.git
    ```
    this will prompt your for your GitHub username and the personal access token (generated in 2.). Then install with

    ```bash
    cd emap-setup
    pip install -e . -r requirements.txt
    cp global-configuration-EXAMPLE.yaml ../global-configuration.yaml
    cd ..
    ```

</details>


4. <details>
    <summary>Modify configuration</summary>
   
    Modify `global-configuration.yaml` with any passwords, usernames and URLs that need to be changed for a live version.
    these will propagate into the individual `xxx-congic-envs` configuration files, which in turn are used 
    by the`application.properties`.
    
    - For example, make sure `UDS_SCHEMA` is set to what it needs to be, in this example I'm using `live`. If you're writing to the UDS, use the `uds_write` user (password in lastpass).
    - If you're running locally, you can set `EMAP_PROJECT_NAME` to whatever you like. If running on the GAE I suggest something like `yourname_dev` or `emaplive` depending on which instance you are manipulating.
    - If you're on the GAE the RabbitMQ password should be strong to help prevent a user/malware outside the GAE from accessing the queue.
    
</details>

5. <details>
    <summary>Clone the repositories</summary>

    Repositories must be checked out to the correct branches. "Correct" will depend on what you're trying to do.
    Conventionally a live instance would all be deployed from master, but during the development phase `develop`
    or a feature branch is more likely to be the correct. Clone all the master branches with:

    ```bash
    emap setup --init --branch master
    ```

    This will result in the following directory structure

    ```bash
    $ tree -L 1
    .
    ├── Emap-Core
    ├── Emap-Interchange
    ├── Inform-DB
    ├── config
    ├── emap-hl7-processor
    ├── global-configuration.yaml
    └── hoover 
   ```

</details>

6. <details>
    <summary>Creating an instance</summary>
   
    ```bash
    emap docker up -d
    ```

    Check the status with 
    ```bash
    emap docker ps
    ```
   
    For example, this may give
    ```
    (develop) $ ./emap.sh ps
    Global Emap config file: /Users/jeremystein/Emap/global-config-envs
    ++ docker-compose -f /Users/jeremystein/Emap/Emap-Core/docker-compose.yml -f /Users/jeremystein/Emap/Emap-Core/docker-compose.fakeuds.yml -f /Users/jeremystein/Emap/Emap-Core/../DatabaseFiller/docker-compose.yml -p jes1 ps
    WARNING: The HTTP_PROXY variable is not set. Defaulting to a blank string.
    WARNING: The http_proxy variable is not set. Defaulting to a blank string.
    WARNING: The HTTPS_PROXY variable is not set. Defaulting to a blank string.
    WARNING: The https_proxy variable is not set. Defaulting to a blank string.
    Name                    Command                State                                               Ports                                           
    ---------------------------------------------------------------------------------------------------------------------------------------------------------
    jes1_emapstar_1    /usr/local/bin/mvn-entrypo ...   Up                                                                                                   
    jes1_fakeuds_1     docker-entrypoint.sh postgres    Up         0.0.0.0:5433->5432/tcp                                                                    
    jes1_hl7source_1   /usr/local/bin/mvn-entrypo ...   Up                                                                                                   
    jes1_rabbitmq_1    docker-entrypoint.sh rabbi ...   Up         15671/tcp, 0.0.0.0:15972->15672/tcp, 25672/tcp, 4369/tcp, 5671/tcp, 0.0.0.0:5972->5672/tcp
    ```
   
</details>


## Miscellaneous

Ports which are allocated per project are listed on the [GAE port log](https://liveuclac.sharepoint.com/sites/RITS-EMAP/_layouts/OneNote.aspx?id=%2Fsites%2FRITS-EMAP%2FSiteAssets%2FInform%20-%20Emap%20Notebook&wd=target%28_Collaboration%20Space%2FOrganisation%20Notes.one%7C3BDBA82E-CB01-45FF-B073-479542EA6D7E%2FGAE%20Port%20Log%7C1C87DFDC-7FCF-4B63-BC51-2BA497BA8DBF%2F%29),
[onenote](https://liveuclac.sharepoint.com/sites/RITS-EMAP/SiteAssets/Inform%20-%20Emap%20Notebook/_Collaboration%20Space/Organisation%20Notes.one#GAE%20Port%20Log&section-id={3BDBA82E-CB01-45FF-B073-479542EA6D7E}&page-id={1C87DFDC-7FCF-4B63-BC51-2BA497BA8DBF}&end). 
