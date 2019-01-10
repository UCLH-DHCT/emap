#!/usr/bin/perl

# Remove unwanted docker images, i.e. where REPOSITORY is <none>

use strict;

my $filename = "./.files.txt";

system ("docker image ls > .files.txt");

open (IN, $filename) or die $!;

# File will typically look like:
# REPOSITORY                      TAG                 IMAGE ID            CREATED             SIZE
# docker_hl7                      latest              491c67f86bc4        7 minutes ago       107MB
# <none>                          <none>              ddfb23276915        31 minutes ago      107MB
# <none>                          <none>              68bfeb122ca1        34 minutes ago      107MB
# <none>                          <none>              cbbe78157e96        38 minutes ago      107MB
# <none>                          <none>              4c68a16c0e5a        2 days ago          107MB


my $line = <IN>; # Skip over header

while ($line = <IN>) {
	my @parts = split (' ', $line);
	my $repo = $parts[0];
	my $image = $parts[2];
	print ("Got $repo, $image\n");
	if ($repo eq "<none>") {
		print ("Deleting docker image $image");
		system ("docker image rm -f $image");
	}
}

close $filename;

system ("rm $filename");

print ("Finished"); 
