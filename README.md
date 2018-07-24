# Kotlin Template
> https://github.com/MikeMitterer/template-kotlin

## Usage
Clone this template

    git clone git@github.com:MikeMitterer/template-kotlin.git <new Project>

Go to cloned dir

    cd <new Project>

Change the root-Project-Name
Make sure the new name is lowercase and has no space in it

    sed -i -e 's/kotlin-templat/<new project name>/' settings.gradle

Remove your `.git` Folder

    rm -rf .git
    
Deploy your lib

    gradle deploy    

        




