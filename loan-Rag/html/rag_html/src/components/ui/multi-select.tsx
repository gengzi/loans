"use client";

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";
import { X } from "lucide-react";

interface MultiSelectProps {
  options: Array<{ value: string; label: string }>;
  selectedValues: string[];
  onSelectionChange: (values: string[]) => void;
  placeholder?: string;
  className?: string;
}

export const MultiSelect = React.forwardRef<
  React.ElementRef<typeof Select>,
  MultiSelectProps
>(({ options, selectedValues, onSelectionChange, placeholder, className }, ref) => {
  const handleRemoveValue = (valueToRemove: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const newValues = selectedValues.filter(value => value !== valueToRemove);
    onSelectionChange(newValues);
  };

  const handleSelectChange = (value: string) => {
    // Toggle selection
    if (selectedValues.includes(value)) {
      onSelectionChange(selectedValues.filter(v => v !== value));
    } else {
      onSelectionChange([...selectedValues, value]);
    }
  };

  return (
    <Select value="" onValueChange={handleSelectChange}>
      <SelectTrigger className={`h-auto min-h-[42px] flex flex-wrap items-center gap-1 p-2 ${className}`}>
        {selectedValues.length > 0 ? (
          <>
            {selectedValues.map((value) => {
              const option = options.find(opt => opt.value === value);
              return (
                <Badge
                  key={value}
                  variant="secondary"
                  className="m-0.5 flex items-center gap-1"
                >
                  {option?.label || value}
                  <button
                    type="button"
                    className="h-3 w-3 flex items-center justify-center rounded-full hover:bg-white/20"
                    onClick={(e) => handleRemoveValue(value, e)}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              );
            })}
          </>
        ) : (
          <SelectValue placeholder={placeholder || "请选择"} />
        )}
      </SelectTrigger>
      <SelectContent className="h-[300px] overflow-y-auto">
        {options.map((option) => (
          <SelectItem 
            key={option.value} 
            value={option.value}
            className={selectedValues.includes(option.value) ? "bg-accent" : ""}
          >
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
});

MultiSelect.displayName = "MultiSelect";