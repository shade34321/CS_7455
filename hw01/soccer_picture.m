addpath E:\Dropbox\School\SPSU_KSU\CS_7455\CS_7455\hw01;
cd E:\Dropbox\School\SPSU_KSU\CS_7455\CS_7455\hw01;
soccer = imread('soccer_field4.jpg');
color = double(soccer);
r = color(:,:, 1);
g = color(:,:, 2);
b = color(:,:, 3);

red = ((r - g > 10) & (r-b > 10));
green = ((g -r > 10) & (g - b > 10));
blue = ((b - r > 10) & (b - g > 10));
red = imdilate(red, ones(7,7));
green = imdilate(green, ones(7,7));
blue = imdilate(green, ones(7,7));

green = remove_holes(green);

%[labels, ~] = bwlabel(green, 4);
%field = (labels == 2);
%field = remove_holes(field);
figure(1);
imshow(field)%

