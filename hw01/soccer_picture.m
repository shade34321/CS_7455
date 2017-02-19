% The next two lines were for me in testing. Just put the filename into the
% variable filename and it should work.
%addpath E:\Dropbox\School\SPSU_KSU\CS_7455\CS_7455\hw01;
%cd E:\Dropbox\School\SPSU_KSU\CS_7455\CS_7455\hw01;
filename = 'E:\Dropbox\School\SPSU_KSU\CS_7455\CS_7455\hw01\data\soccer_field4.jpg';
soccer = imread(filename); %read in the file.

% Extract all three color bands into their own respective spots.
color = double(soccer); 
r = color(:,:, 1);
g = color(:,:, 2);
b = color(:,:, 3);

% Identify all the green areas. Playing with this gives various results. 
green = ((g - r > 50) & (g - b > 50));
green = imdilate(green, ones(7,7)); %dilate the image a bit. Based off code from the slides.
[labels, ~] = bwlabel(green, 4);
field = (labels == 2);
field = remove_holes(field);

% Here we filter out some more noise and then identify all the red and blue
% areas
green = imclose(green, ones(7,7));
green = imerode(green, ones(7,7));
red = ((r - g > 100) & (r - b > 100));
blue = ((b - r > 50) & (b - g > 50));

% Since we dilated the green the green is also included with the red and
% blue section.
% I didn't dilate the red and blue since it seemed to make blocks and
% return an ugly result.
red_players = (red & green);
blue_players = (blue & green);

% Display the field
figure(1);
imshow(field);

% Display the red players
figure(2);
imshow(red_players);

% Display the blue players
figure(3);
imshow(blue_players);