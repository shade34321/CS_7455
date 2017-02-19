% remove_holes Removes holes from a black and white image rendering objects
%              in the foreground one color with the background a different
%              color
% test_zebra = remove_holes(zebra)
% Where zebra is a binary image and the test_zebra is the image with the holes
% removed.
%
% Author Shade Alabsa
% Version 1.0
% Date 17 February 2017

function holeless_image = remove_holes(input_image)
    % Use the inverse of the image to get a lable matrix and number of
    % connected objects.
    [labels, number] = bwlabel(~input_image, 4);

    % Allows us to store how many pixels per component.
    counters = zeros(1,number);
    for i = 1:number
        % for each i, we count the number of pixels equal to i in the labels
        % matrix
        % first, we create a component image, that is 1 for pixels belonging to
        % the i-th connected component, and 0 everywhere else.
        component_image = (labels == i);

        % now, we count the non-zero pixels in the component image.
        counters(i) = sum(component_image(:));
    end

    % find the id of the largest component
    % We don't care about the area so we use ~ to ignore it
    % Then we return the inverse so the background is black again.
    [~, id] = max(counters);    
    holeless_image = ~(labels == id);
end